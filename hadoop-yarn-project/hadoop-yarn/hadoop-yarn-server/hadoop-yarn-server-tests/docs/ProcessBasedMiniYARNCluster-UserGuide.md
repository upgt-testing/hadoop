# ProcessBasedMiniYARNCluster User Guide

**Version**: 1.0
**Last Updated**: 2025-11-09
**Target Audience**: YARN developers, test engineers, upgrade testers

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Basic Usage](#basic-usage)
5. [Multi-Version Testing](#multi-version-testing)
6. [High Availability](#high-availability)
7. [Rolling Upgrades](#rolling-upgrades)
8. [Configuration](#configuration)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)
11. [API Reference](#api-reference)

---

## Overview

### What is ProcessBasedMiniYARNCluster?

ProcessBasedMiniYARNCluster is a process-based version of Hadoop YARN's MiniYARNCluster designed specifically for **multi-version upgrade testing**. Unlike the original MiniYARNCluster which runs all YARN nodes in a single JVM, ProcessBasedMiniYARNCluster runs each ResourceManager and NodeManager in **separate JVM processes** with **isolated classpaths**.

### Key Features

- **Process Isolation**: Each YARN node runs in its own JVM process
- **Multi-Version Support**: Different nodes can run different Hadoop versions (e.g., RM on 3.3.6, NMs on 3.4.0)
- **RPC-Only Communication**: Test code interacts with cluster via YarnClient APIs only
- **Rolling Upgrade Testing**: Simulate production upgrade scenarios
- **HA Support**: ResourceManager High Availability with automatic failover
- **Dynamic Port Allocation**: Automatic port assignment to avoid conflicts
- **Builder Pattern API**: Fluent, type-safe cluster configuration

### When to Use ProcessBasedMiniYARNCluster

**Use ProcessBasedMiniYARNCluster when:**
- Testing YARN upgrades between versions (3.3.x → 3.4.x)
- Verifying version compatibility
- Simulating rolling upgrades
- Testing mixed-version cluster operations
- Reproducing production upgrade issues

**Use MiniYARNCluster when:**
- Running standard YARN functional tests
- Testing within a single Hadoop version
- Performance is critical (in-JVM is faster)
- Direct RM/NM object access is needed

---

## Architecture

### Process Model

```
Test JVM Process
├── ProcessBasedMiniYARNCluster (coordinator)
│   ├── DirectoryManager (temp directories)
│   ├── PortAllocator (port assignments)
│   ├── ProcessConfigurationGenerator (config files)
│   └── HadoopVersionRegistry (distribution catalog)
│
└── Subprocess Management
    ├── ResourceManager Process 0 (separate JVM)
    │   ├── Hadoop 3.3.6 classpath
    │   ├── yarn-site.xml (rm0 config)
    │   └── RPC: 0.0.0.0:50001
    │
    ├── ResourceManager Process 1 (separate JVM, HA mode)
    │   ├── Hadoop 3.3.6 classpath
    │   ├── yarn-site.xml (rm1 config)
    │   └── RPC: 0.0.0.0:50002
    │
    ├── NodeManager Process 0 (separate JVM)
    │   ├── Hadoop 3.4.0 classpath  ← Different version!
    │   ├── yarn-site.xml (nm0 config)
    │   └── RPC: 0.0.0.0:50010
    │
    └── NodeManager Process 1 (separate JVM)
        ├── Hadoop 3.4.0 classpath
        ├── yarn-site.xml (nm1 config)
        └── RPC: 0.0.0.0:50011

YarnClient (in Test JVM)
└── RPC → ResourceManager RPC endpoints
```

### Directory Structure

```
/tmp/process-miniyarn-<timestamp>/
├── rm0/
│   ├── conf/
│   │   ├── yarn-site.xml
│   │   └── core-site.xml
│   ├── data/
│   │   └── rm-state-store/
│   ├── logs/
│   │   ├── stdout.log
│   │   └── stderr.log
│   └── pid
├── rm1/ (if HA enabled)
│   └── ...
├── nm0/
│   ├── conf/
│   ├── logs/
│   ├── local-dirs/
│   │   ├── nm-local-dir/
│   │   └── usercache/
│   └── log-dirs/
└── nm1/
    └── ...
```

### Communication Model

**Test Code ↔ YARN Cluster:**
- All communication via **YarnClient RPC APIs**
- No direct ResourceManager/NodeManager object access
- Socket-based health checks (no YarnClient dependency in cluster code)

**YARN Nodes ↔ YARN Nodes:**
- Standard YARN RPC protocols
- Each node configured with other nodes' RPC addresses

---

## Getting Started

### Prerequisites

1. **Multiple Hadoop Distributions Built** (for multi-version testing)
   ```bash
   # Build Hadoop 3.3.6
   cd /path/to/hadoop-3.3.6-src
   mvn clean install -DskipTests

   # Build Hadoop 3.4.0
   cd /path/to/hadoop-3.4.0-src
   mvn clean install -DskipTests
   ```

2. **Set System Properties** (optional, for default versions)
   ```bash
   export MAVEN_OPTS="-Dhadoop.start.home=/opt/hadoop-3.3.6 \
                      -Dhadoop.upgrade.home=/opt/hadoop-3.4.0"
   ```

3. **Add Test Dependency**
   ```xml
   <dependency>
     <groupId>org.apache.hadoop</groupId>
     <artifactId>hadoop-yarn-server-tests</artifactId>
     <version>3.3.6</version>
     <scope>test</scope>
     <classifier>tests</classifier>
   </dependency>
   ```

### Quick Start Example

```java
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.server.process.ProcessBasedMiniYARNCluster;

public class QuickStartExample {
    public static void main(String[] args) throws Exception {
        // 1. Create cluster configuration
        YarnConfiguration conf = new YarnConfiguration();

        // 2. Build cluster with 1 RM + 3 NMs
        ProcessBasedMiniYARNCluster cluster =
            new ProcessBasedMiniYARNCluster.Builder(conf)
                .numResourceManagers(1)
                .numNodeManagers(3)
                .build();

        try {
            // 3. Start the cluster
            cluster.start();

            // 4. Create YarnClient
            YarnClient yarnClient = YarnClient.createYarnClient();
            yarnClient.init(cluster.getConfiguration());
            yarnClient.start();

            try {
                // 5. Use the cluster
                System.out.println("Cluster Metrics: " +
                    yarnClient.getYarnClusterMetrics());

                // Submit applications, run tests, etc.

            } finally {
                yarnClient.stop();
            }
        } finally {
            // 6. Shutdown cluster
            cluster.shutdown();
        }
    }
}
```

---

## Basic Usage

### Creating a Single-Version Cluster

**Standalone Mode (1 ResourceManager):**

```java
YarnConfiguration conf = new YarnConfiguration();

ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .build();

cluster.start();
// Use cluster...
cluster.shutdown();
```

**With try-with-resources (Recommended):**

```java
YarnConfiguration conf = new YarnConfiguration();

try (ProcessBasedMiniYARNCluster cluster =
         new ProcessBasedMiniYARNCluster.Builder(conf)
             .numResourceManagers(1)
             .numNodeManagers(3)
             .build()) {

    cluster.start();

    // Use cluster...

} // Automatic shutdown
```

### Accessing the Cluster via YarnClient

```java
// Create and start YarnClient
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

try {
    // Get cluster metrics
    YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
    System.out.println("Total nodes: " + metrics.getNumNodeManagers());

    // Get node reports
    List<NodeReport> nodes = yarnClient.getNodeReports(NodeState.RUNNING);
    for (NodeReport node : nodes) {
        System.out.println("Node: " + node.getNodeId() +
                         ", State: " + node.getNodeState());
    }

    // Submit application
    ApplicationId appId = submitApplication(yarnClient);

    // Monitor application
    ApplicationReport report = yarnClient.getApplicationReport(appId);
    System.out.println("App State: " + report.getYarnApplicationState());

} finally {
    yarnClient.stop();
}
```

### Restarting Nodes

```java
ProcessBasedMiniYARNCluster cluster = ...;
cluster.start();

// Restart ResourceManager 0
cluster.restartResourceManager(0);

// Restart NodeManager 1
cluster.restartNodeManager(1);

// Wait for nodes to reconnect
Thread.sleep(5000);
```

---

## Multi-Version Testing

### Setting Up Multi-Version Environment

**Method 1: System Properties**

```bash
mvn test -Dtest=MyUpgradeTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.6 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
```

**Method 2: Programmatic Configuration**

```java
// Create version registry
HadoopVersionRegistry registry = new HadoopVersionRegistry();
registry.register("3.3.6", "/opt/hadoop-3.3.6");
registry.register("3.4.0", "/opt/hadoop-3.4.0");

// Build cluster with specific versions
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")  // RM on 3.3.6
        .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")  // NM0 on 3.4.0
        .nmHadoopDistribution(1, "/opt/hadoop-3.4.0")  // NM1 on 3.4.0
        .nmHadoopDistribution(2, "/opt/hadoop-3.4.0")  // NM2 on 3.4.0
        .build();
```

**Method 3: Bulk Assignment**

```java
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.6")  // All nodes start at 3.3.6
        .build();

cluster.start();

// Later upgrade individual nodes...
```

### Version Compatibility Matrix

| Scenario | RM Version | NM Version | Supported? | Notes |
|----------|-----------|-----------|-----------|-------|
| Same major.minor | 3.3.6 | 3.3.5 | ✅ Fully | Patch versions always compatible |
| Same major.minor | 3.4.0 | 3.4.1 | ✅ Fully | Patch versions always compatible |
| Different minor | 3.3.6 | 3.4.0 | ✅ Compatible | Protocol compatible, test carefully |
| Different minor | 3.4.0 | 3.3.6 | ✅ Compatible | Protocol compatible, test carefully |
| Different major | 2.10.0 | 3.3.6 | ❌ No | Major version incompatibility |
| Different major | 3.3.6 | 4.0.0 | ❌ No | Major version incompatibility |

**Check Compatibility Programmatically:**

```java
boolean compatible = VersionConfigAdapter.areCompatible("3.3.6", "3.4.0");
if (compatible) {
    System.out.println("Versions are compatible");
} else {
    System.out.println("WARNING: Versions may be incompatible");
}
```

---

## High Availability

### Creating HA Cluster

```java
YarnConfiguration conf = new YarnConfiguration();

ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)  // HA requires ≥ 2 RMs
        .numNodeManagers(3)
        .build();

cluster.start();

// Check which RM is active
int activeIndex = cluster.getActiveRMIndex();
System.out.println("Active RM: " + activeIndex);
```

### Manual Failover

```java
// Get current active RM
int activeRM = cluster.getActiveRMIndex();

// Restart active RM (triggers failover)
cluster.restartResourceManager(activeRM);

// Wait for failover to complete
Thread.sleep(5000);

// Verify new active RM
int newActiveRM = cluster.getActiveRMIndex();
System.out.println("Failover: RM" + activeRM + " → RM" + newActiveRM);
```

### HA Configuration

The cluster automatically configures HA when numResourceManagers > 1:

```xml
<!-- Auto-generated yarn-site.xml -->
<property>
  <name>yarn.resourcemanager.ha.enabled</name>
  <value>true</value>
</property>

<property>
  <name>yarn.resourcemanager.ha.rm-ids</name>
  <value>rm0,rm1</value>
</property>

<property>
  <name>yarn.resourcemanager.hostname.rm0</name>
  <value>localhost</value>
</property>

<property>
  <name>yarn.resourcemanager.address.rm0</name>
  <value>localhost:50001</value>
</property>

<property>
  <name>yarn.resourcemanager.hostname.rm1</name>
  <value>localhost</value>
</property>

<property>
  <name>yarn.resourcemanager.address.rm1</name>
  <value>localhost:50002</value>
</property>
```

---

## Rolling Upgrades

### Using UpgradeTestHelper

```java
import org.apache.hadoop.yarn.server.process.upgrade.UpgradeTestHelper;

// Create cluster with 1 RM + 3 NMs (all on 3.3.6)
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
        .build();

cluster.start();

// Create upgrade helper
UpgradeTestHelper helper = new UpgradeTestHelper(cluster);

// Perform rolling upgrade of all NMs (one-by-one)
helper.rollingUpgradeAllNodeManagers("/opt/hadoop-3.4.0", 5000);

// Now: RM on 3.3.6, all NMs on 3.4.0
```

### Manual Rolling Upgrade

```java
// Start cluster (all nodes on 3.3.6)
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
        .build();

cluster.start();

// Submit applications before upgrade
ApplicationId app = submitTestApplication(yarnClient);

// Upgrade NodeManagers one-by-one
for (int i = 0; i < 3; i++) {
    System.out.println("Upgrading NM " + i + "...");

    // TODO: Current limitation - need cluster API enhancement
    // to change Hadoop distribution per node
    // For now, restart with same version
    cluster.restartNodeManager(i);

    // Wait for NM to reconnect
    Thread.sleep(5000);

    // Verify cluster still healthy
    YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
    System.out.println("Active NMs: " + metrics.getNumNodeManagers());
}

// Verify application still running
ApplicationReport report = yarnClient.getApplicationReport(app);
System.out.println("App state after upgrade: " +
    report.getYarnApplicationState());
```

### RM HA Upgrade

```java
// Start HA cluster (2 RMs + 3 NMs, all on 3.3.6)
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
        .build();

cluster.start();

UpgradeTestHelper helper = new UpgradeTestHelper(cluster);

// Upgrade RM with automatic failover
helper.upgradeResourceManagerWithFailover(0, "/opt/hadoop-3.4.0");

// Now: Both RMs on 3.4.0, NMs on 3.3.6
```

---

## Configuration

### Builder Options

```java
ProcessBasedMiniYARNCluster.Builder builder =
    new ProcessBasedMiniYARNCluster.Builder(conf);

// Number of nodes
builder.numResourceManagers(2);  // Default: 1
builder.numNodeManagers(5);      // Default: 1

// Hadoop distributions
builder.rmHadoopDistribution(0, "/opt/hadoop-3.3.6");
builder.rmHadoopDistribution(1, "/opt/hadoop-3.4.0");
builder.nmHadoopDistribution(0, "/opt/hadoop-3.4.0");
builder.allNodesHadoopDistribution("/opt/hadoop-3.3.6");

// Build cluster
ProcessBasedMiniYARNCluster cluster = builder.build();
```

### YarnConfiguration Overrides

```java
YarnConfiguration conf = new YarnConfiguration();

// Memory settings
conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
conf.setInt(YarnConfiguration.RM_SCHEDULER_MAXIMUM_ALLOCATION_MB, 2048);
conf.setInt(YarnConfiguration.NM_PMEM_MB, 4096);

// Timeout settings
conf.setLong(YarnConfiguration.RM_NM_EXPIRY_INTERVAL_MS, 30000);

// Security (for testing)
conf.setBoolean(YarnConfiguration.YARN_ACL_ENABLE, false);

ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(3)
        .build();
```

### Directory Management

```java
// Default: Cluster directories created under /tmp/process-miniyarn-<timestamp>/

// Keep directories after shutdown (for debugging)
DirectoryManager dirManager = cluster.getDirectoryManager();
dirManager.setDeleteOnCleanup(false);

// Get cluster root directory
File clusterRoot = dirManager.getClusterRoot();
System.out.println("Cluster root: " + clusterRoot);

// Get RM directory
File rmDir = cluster.getResourceManagerDirectory(0);
System.out.println("RM0 logs: " + new File(rmDir, "logs"));

// Get NM directory
File nmDir = cluster.getNodeManagerDirectory(0);
System.out.println("NM0 logs: " + new File(nmDir, "logs"));
```

---

## Troubleshooting

### Issue: Cluster Fails to Start

**Symptoms:**
```
TimeoutException: Timed out waiting for ResourceManager to become ready
```

**Solutions:**

1. **Check Hadoop Distribution Validity:**
   ```bash
   ls -l /opt/hadoop-3.3.6/share/hadoop/yarn
   # Should contain: hadoop-yarn-server-*.jar
   ```

2. **Check Port Availability:**
   ```bash
   netstat -an | grep 50001
   # If port in use, another cluster may be running
   ```

3. **Increase Startup Timeout:**
   ```java
   // Increase health check timeout (default: 30s)
   HealthMonitor monitor = new HealthMonitor();
   monitor.setHealthTimeout(60000);  // 60 seconds
   ```

4. **Check Process Logs:**
   ```bash
   # Cluster root printed at startup
   tail -f /tmp/process-miniyarn-*/rm0/logs/stderr.log
   ```

### Issue: NodeManagers Not Connecting

**Symptoms:**
```
YarnClusterMetrics shows 0 NodeManagers
```

**Solutions:**

1. **Wait Longer for Registration:**
   ```java
   cluster.start();
   Thread.sleep(10000);  // Wait 10 seconds
   ```

2. **Check NM Logs:**
   ```bash
   tail -f /tmp/process-miniyarn-*/nm0/logs/stderr.log
   # Look for connection errors
   ```

3. **Verify RM Address Configuration:**
   ```java
   YarnConfiguration nmConf = cluster.getNodeManagerConfiguration(0);
   System.out.println("RM address: " +
       nmConf.get(YarnConfiguration.RM_ADDRESS));
   ```

### Issue: Version Compatibility Errors

**Symptoms:**
```
RPC incompatibility errors in logs
```

**Solutions:**

1. **Check Version Compatibility:**
   ```java
   boolean compatible = VersionConfigAdapter.areCompatible("3.3.6", "3.4.0");
   System.out.println("Compatible: " + compatible);
   ```

2. **Use Version Adapter:**
   ```java
   VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
   YarnConfiguration adapted = adapter.adapt(conf, "3.3.6");
   ```

3. **Check Documentation:**
   - See `VERSION_COMPATIBILITY.md` for known compatible versions

### Issue: Processes Not Cleaning Up

**Symptoms:**
```
Orphaned java processes after test completion
```

**Solutions:**

1. **Always Use try-with-resources:**
   ```java
   try (ProcessBasedMiniYARNCluster cluster = ...) {
       cluster.start();
       // Use cluster
   } // Automatic cleanup
   ```

2. **Ensure shutdown() Called:**
   ```java
   ProcessBasedMiniYARNCluster cluster = null;
   try {
       cluster = new ProcessBasedMiniYARNCluster.Builder(conf).build();
       cluster.start();
   } finally {
       if (cluster != null) {
           cluster.shutdown();
       }
   }
   ```

3. **Kill Orphaned Processes:**
   ```bash
   # Find orphaned processes
   ps aux | grep ResourceManagerProcessLauncher

   # Kill if needed
   kill <pid>
   ```

---

## Best Practices

### 1. Always Use try-with-resources

```java
// ✅ GOOD
try (ProcessBasedMiniYARNCluster cluster = builder.build()) {
    cluster.start();
    // Use cluster
} // Automatic cleanup

// ❌ BAD
ProcessBasedMiniYARNCluster cluster = builder.build();
cluster.start();
// Might leak if exception thrown
cluster.shutdown();
```

### 2. Set Reasonable Timeouts for Tests

```java
YarnConfiguration conf = new YarnConfiguration();

// Faster timeouts for test environments
conf.setLong(YarnConfiguration.RM_NM_EXPIRY_INTERVAL_MS, 10000);
conf.setInt(YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 1000);
```

### 3. Wait for Cluster Readiness

```java
cluster.start();

// Wait for NMs to register
YarnClient yarnClient = YarnClient.createYarnClient();
yarnClient.init(cluster.getConfiguration());
yarnClient.start();

GenericTestUtils.waitFor(() -> {
    try {
        return yarnClient.getYarnClusterMetrics().getNumNodeManagers() == 3;
    } catch (Exception e) {
        return false;
    }
}, 1000, 30000);
```

### 4. Use System Properties for CI/CD

```yaml
# .github/workflows/upgrade-tests.yml
- name: Run Upgrade Tests
  run: |
    mvn test -Dtest='*UpgradeTest' \
      -Dhadoop.start.home=/opt/hadoop-3.3.6 \
      -Dhadoop.upgrade.home=/opt/hadoop-3.4.0
```

### 5. Keep Logs for Failed Tests

```java
@Rule
public TestName testName = new TestName();

@After
public void tearDown() {
    if (cluster != null) {
        DirectoryManager dirManager = cluster.getDirectoryManager();

        // Keep logs if test failed
        if (testFailed()) {
            dirManager.setDeleteOnCleanup(false);
            System.out.println("Test failed. Logs at: " +
                dirManager.getClusterRoot());
        }

        cluster.shutdown();
    }
}
```

### 6. Minimize Cluster Size

```java
// ✅ GOOD - Minimal cluster for unit tests
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(1)
        .numNodeManagers(1)  // 1 NM sufficient for many tests
        .build();

// ❌ BAD - Unnecessarily large
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(3)
        .numNodeManagers(10)  // Slow startup, more resource usage
        .build();
```

### 7. Version Control for Test Distributions

```bash
# Use specific version paths
-Dhadoop.start.home=/opt/hadoop-releases/hadoop-3.3.6
-Dhadoop.upgrade.home=/opt/hadoop-releases/hadoop-3.4.0

# Not generic "latest" symlinks
# -Dhadoop.start.home=/opt/hadoop-latest  ❌
```

---

## API Reference

### ProcessBasedMiniYARNCluster

**Lifecycle Methods:**
```java
void start() throws IOException, TimeoutException
void shutdown() throws IOException
void close() throws IOException  // Closeable interface
```

**Node Management:**
```java
void restartResourceManager(int rmIndex) throws IOException, TimeoutException
void restartNodeManager(int nmIndex) throws IOException, TimeoutException
int getNumResourceManagers()
int getNumNodeManagers()
int getActiveRMIndex() throws IOException
boolean isClusterUp()
```

**Configuration Access:**
```java
YarnConfiguration getConfiguration()
YarnConfiguration getResourceManagerConfiguration(int rmIndex)
YarnConfiguration getNodeManagerConfiguration(int nmIndex)
```

**Directory Access:**
```java
File getClusterRoot()
File getResourceManagerDirectory(int rmIndex)
File getNodeManagerDirectory(int nmIndex)
DirectoryManager getDirectoryManager()
```

**Unsupported Methods** (throw UnsupportedOperationException):
```java
Object getResourceManager()  // ❌ Use YarnClient instead
Object getNodeManager(int)   // ❌ Use YarnClient.getNodeReports()
```

### ProcessBasedMiniYARNCluster.Builder

```java
Builder(YarnConfiguration conf)
Builder numResourceManagers(int num)
Builder numNodeManagers(int num)
Builder rmHadoopDistribution(int rmIndex, String hadoopHome)
Builder nmHadoopDistribution(int nmIndex, String hadoopHome)
Builder allNodesHadoopDistribution(String hadoopHome)
ProcessBasedMiniYARNCluster build() throws IOException
```

### UpgradeTestHelper

```java
UpgradeTestHelper(ProcessBasedMiniYARNCluster cluster)

void rollingUpgradeAllNodeManagers(String newHadoopHome)
    throws IOException, TimeoutException, InterruptedException
void rollingUpgradeAllNodeManagers(String newHadoopHome, long waitBetweenRestarts)
    throws IOException, TimeoutException, InterruptedException

void upgradeNodeManager(int nmIndex, String newHadoopHome)
    throws IOException, TimeoutException

void upgradeResourceManagerWithFailover(int rmIndex, String newHadoopHome)
    throws IOException, TimeoutException

void batchUpgradeNodeManagers(int[] nmIndices, String newHadoopHome)
    throws IOException, TimeoutException

boolean verifyClusterHealthy()

static boolean areVersionsCompatible(String version1, String version2)
static String getUpgradePath(String fromVersion, String toVersion)

void setRestartDelay(long delayMs)
void setHealthTimeout(long timeoutMs)
```

### VersionConfigAdapter

```java
static VersionConfigAdapter forVersion(String targetVersion)
static boolean areCompatible(String version1, String version2)

YarnConfiguration adapt(Configuration sourceConfig, String sourceVersion)
void applyDefaults(Configuration config)
String getTargetVersion()
```

---

## See Also

- **Developer Guide**: `ProcessBasedMiniYARNCluster-DeveloperGuide.md` - Architecture details, extending the framework
- **Version Compatibility**: `VERSION_COMPATIBILITY.md` - Comprehensive upgrade testing guide
- **Source Code**: `org.apache.hadoop.yarn.server.process.*` - Implementation reference

---

**Document Version**: 1.0
**Last Updated**: 2025-11-09
**Feedback**: Report issues to YARN development team
