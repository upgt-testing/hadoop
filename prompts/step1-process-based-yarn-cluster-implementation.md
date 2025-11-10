# Step 1: ProcessBasedMiniYARNCluster Implementation Guide

## Executive Summary

This document outlines the plan to create `ProcessBasedMiniYARNCluster` that extends the existing `MiniYARNCluster` functionality but runs each node in separate JVM processes. This enables testing version compatibility and upgrade scenarios by allowing different nodes to run different versions.

**Project Information:**
- **Project Name**: Apache Hadoop YARN
- **Original Cluster Class**: MiniYARNCluster
- **New Cluster Class**: ProcessBasedMiniYARNCluster
- **Project Root**: hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests

**Node Types in YARN Cluster:**
- **Node Type 1**: ResourceManager - Class: ResourceManager - Role: Cluster resource manager, application coordinator, and scheduler
- **Node Type 2**: NodeManager - Class: NodeManager - Role: Worker node that runs application containers
- **Optional**: ApplicationHistoryServer - Class: ApplicationHistoryServer - Role: Historical application data storage

---

## Goals

1. **Process Isolation**: Each YARN node runs in its own JVM process
2. **Version Flexibility**: Support running different Hadoop versions for different nodes
3. **API Compatibility**: Extend existing MiniYARNCluster API where possible
4. **Client-Side Only**: Support only client-side operations (RPC/HTTP based)
5. **Testing Focus**: Enable version upgrade and compatibility testing
6. **RM HA Support**: Support ResourceManager High Availability upgrade scenarios

## Non-Goals (Initial Phase)

- Performance optimization - correctness over speed
- Hot-swap/runtime version changes - versions set at cluster creation
- Cross-version internal state compatibility testing (may be future work)
- Components outside the core cluster (Timeline Service v2, Federation)

---

## Architecture Overview

### Current MiniYARNCluster Architecture

```
┌─────────────────────────────────────────┐
│          Test JVM Process               │
│  ┌─────────────────────────────────┐   │
│  │     MiniYARNCluster             │   │
│  │  ┌──────────┐  ┌──────────┐    │   │
│  │  │RM        │  │NM        │    │   │
│  │  │(object)  │  │(object)  │    │   │
│  │  └──────────┘  └──────────┘    │   │
│  │                                 │   │
│  │  Direct method calls possible   │   │
│  └─────────────────────────────────┘   │
│  Shared classpath & version            │
└─────────────────────────────────────────┘
```

### New ProcessBasedMiniYARNCluster Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Test JVM Process                         │
│  ┌───────────────────────────────────────────────────┐     │
│  │     ProcessBasedMiniYARNCluster                   │     │
│  │                                                     │     │
│  │  ┌────────────────┐  ┌────────────────┐          │     │
│  │  │ RM Process     │  │ NM Process     │          │     │
│  │  │ Mgr (Control)  │  │ Mgr (Control)  │          │     │
│  │  └────────┬───────┘  └────────┬───────┘          │     │
│  │           │ RPC                 │ RPC             │     │
│  └───────────┼─────────────────────┼─────────────────┘     │
│              │                     │                        │
└──────────────┼─────────────────────┼────────────────────────┘
               │                     │
       ┌───────▼────────┐    ┌──────▼─────────┐
       │  ResourceMgr   │    │  NodeManager   │
       │  Process       │    │  Process       │
       │                │    │                │
       │ Hadoop 3.3.x   │    │ Hadoop 3.4.x   │
       │ (Isolated CP)  │    │ (Isolated CP)  │
       └────────────────┘    └────────────────┘
```

**Key Components:**

1. **ProcessBasedMiniYARNCluster**: Main cluster coordinator (in test JVM)
2. **Process Managers**: Start/stop/monitor individual RM and NM processes
3. **Node Launchers**: Entry points for RM and NM processes
4. **RPC Clients**: Communicate with nodes via YARN RPC protocols
5. **Configuration Manager**: Generate and distribute configs per node
6. **Classpath Isolator**: Ensure each process uses correct Hadoop version

---

## Detailed Design

### 1. Class Structure

#### 1.1 Main Classes

```
ProcessBasedMiniYARNCluster (extends MiniYARNCluster or provides similar API)
├── ProcessNodeManager
│   ├── ResourceManagerProcessManager
│   ├── NodeManagerProcessManager
│   └── ApplicationHistoryServerProcessManager (optional)
├── HadoopVersionRegistry
├── ProcessConfigurationGenerator
└── ProcessLauncher
    ├── ResourceManagerProcessLauncher (Main class for subprocess)
    ├── NodeManagerProcessLauncher (Main class for subprocess)
    └── ApplicationHistoryServerProcessLauncher (optional)
```

#### 1.2 ProcessBasedMiniYARNCluster

**Responsibilities:**
- HIGHEST PRIORITY: Follow and transform MiniYARNCluster API to support/un-support methods as needed
- Follow the same creation/startup/shutdown patterns as MiniYARNCluster
- Manage lifecycle of all YARN processes
- Provide client-side API access only. If not possible, then those methods should tag as unsupported and throw `UnsupportedOperationException`.
- Throw UnsupportedOperationException for direct object access methods

**New Builder Options (extends MiniYARNCluster.Builder):**
```java
Builder hadoopDistribution(int nodeIndex, String hadoopHome)
Builder allNodesHadoopDistribution(String hadoopHome)
Builder rmHadoopDistribution(int rmIndex, String hadoopHome)
Builder nmHadoopDistribution(int nmIndex, String hadoopHome)
Builder enableProcessIsolation(boolean enable) // default: true
Builder enableAHS(boolean enable) // Application History Server
```

**Unsupported Methods (will throw UnsupportedOperationException):**
```java
// Methods that return server-side objects
getResourceManager()                        // returns ResourceManager object
getResourceManager(int index)               // returns ResourceManager object
getNodeManager(int index)                   // returns NodeManager object
getApplicationHistoryServer()               // returns ApplicationHistoryServer object

// Direct internal access methods that have no client equivalent
// (These require inspecting internal RM or NM state)
```

**Supported Methods:**
```java
// Client-side operations
createYarnClient()                         // returns YarnClient
getConfig()                                // returns Configuration
waitForNodeManagersToConnect(timeout)      // waits via RPC polling
shutdown()                                 // stops all processes
restartResourceManager(int index)          // stops and restarts RM process
restartNodeManager(int index)              // stops and restarts NM process
shutdownResourceManager(int index)         // stops RM process
shutdownNodeManager(int index)             // stops NM process
startResourceManager(int index)            // starts RM process
startNodeManager(int index)                // starts NM process

// Cluster status
getActiveRMIndex()                         // finds active RM via RPC
getNumResourceManagers()                   // count
getNumNodeManagers()                       // count
isClusterUp()                              // checks via RPC

// Upgrade operations
rollingUpgradeNodeManagers()               // performs NM rolling upgrade
upgradeResourceManager(int index, path)    // upgrades specific RM
changeNodeManagerVersion(int index, path)  // changes NM version
```

#### 1.3 ProcessNodeManager

**Base class for managing node processes:**

```java
abstract class ProcessNodeManager {
    protected Process process;
    protected YarnConfiguration nodeConfig;
    protected String hadoopHome;
    protected File workDir;
    protected int nodeIndex;

    abstract void start() throws IOException;
    abstract void stop() throws IOException;
    abstract boolean isHealthy() throws IOException;
    abstract InetSocketAddress getRpcAddress();

    protected void waitForProcessReady(long timeoutMs);
    protected void killProcess();
    protected List<String> buildClasspath();
}
```

**ResourceManagerProcessManager:**
- Starts ResourceManager process with isolated classpath
- Monitors health via ApplicationClientProtocol RPC
- Handles RM-specific configuration (ports, HA setup, etc.)
- Supports HA mode with standby RM

**NodeManagerProcessManager:**
- Starts NodeManager process with isolated classpath
- Monitors health via ResourceTracker protocol
- Handles NM-specific configuration (local dirs, resource allocation)

**ApplicationHistoryServerProcessManager (optional):**
- Starts AHS process
- Monitors via Timeline service protocol

#### 1.4 HadoopVersionRegistry

**Manages Hadoop distribution locations:**

```java
class HadoopVersionRegistry {
    private Map<String, HadoopDistribution> distributions;

    void register(String version, String hadoopHome);
    HadoopDistribution get(String version);
    List<File> getJars(String version);
    List<File> getDependencies(String version);
}

class HadoopDistribution {
    String version;
    File hadoopHome;
    List<File> yarnJars;        // hadoop-yarn-*.jar
    List<File> commonJars;      // hadoop-common, hadoop-hdfs jars
    List<File> dependencies;    // all lib/*.jar
}
```

**Classpath Construction Strategy:**
```
For each YARN node process:
1. JVM bootstrap classpath (Java runtime)
2. Test framework jars (JUnit, shared test utilities)
3. Node-specific jars from hadoopHome/share/hadoop/yarn/
4. Node-specific jars from hadoopHome/share/hadoop/common/
5. Node-specific dependencies from hadoopHome/share/hadoop/yarn/lib/
6. Node-specific dependencies from hadoopHome/share/hadoop/common/lib/
7. Configuration directory
```

**Dependency Hell Mitigation:**
- Each process gets completely isolated classpath
- No class sharing between processes (except JVM and test framework)
- Communication only via YARN RPC protocols
- Use separate temp directories for each node
- Version-specific native libraries via java.library.path

#### 1.5 ProcessConfigurationGenerator

**Generates configuration files for each YARN node:**

```java
class ProcessConfigurationGenerator {
    Configuration generateResourceManagerConfig(
        Configuration baseConfig,
        int rmIndex,
        File workDir,
        boolean isHAEnabled,
        String[] rmIds);

    Configuration generateNodeManagerConfig(
        Configuration baseConfig,
        int nmIndex,
        File workDir,
        InetSocketAddress rmAddress);

    File writeConfigToFile(Configuration conf, File dir);
}
```

**Configuration File Strategy:**
- Each node gets a unique configuration directory
- Write yarn-site.xml and core-site.xml config files
- Auto-assign ports (scan for free ports)
- Set up isolated data directories:
  - RM: state-store directory
  - NM: local-dirs, log-dirs
- Include version-specific configuration adjustments

#### 1.6 ProcessLauncher (Entry Points)

**ResourceManagerProcessLauncher** (runs in subprocess):
```java
public class ResourceManagerProcessLauncher {
    public static void main(String[] args) {
        // Parse args: --config-dir, --rm-index, --ha-enabled, etc.
        // Load configuration from config-dir
        // Create and start ResourceManager instance
        // Set up signal handlers for graceful shutdown
        // Wait indefinitely (RM runs until shutdown signal)
    }
}
```

**NodeManagerProcessLauncher** (runs in subprocess):
```java
public class NodeManagerProcessLauncher {
    public static void main(String[] args) {
        // Parse args: --config-dir, --nm-index, etc.
        // Load configuration from config-dir
        // Create and start NodeManager instance
        // Set up signal handlers
        // Wait indefinitely (NM runs until shutdown signal)
    }
}
```

**Process Launch Command Example:**
```bash
java \
  -cp /path/to/hadoop-3.3.x/share/hadoop/yarn/*:/path/to/hadoop-3.3.x/share/hadoop/yarn/lib/*:/path/to/hadoop-3.3.x/share/hadoop/common/*:/path/to/hadoop-3.3.x/share/hadoop/common/lib/* \
  -Djava.library.path=/path/to/hadoop-3.3.x/lib/native \
  org.apache.hadoop.yarn.server.ResourceManagerProcessLauncher \
  --config-dir /tmp/process-miniyarn-123456/rm0/conf \
  --rm-index 0
```

---

### 2. Process Management

#### 2.1 Process Startup Sequence

1. **Validate distributions**
   - Check hadoopHome exists
   - Verify required YARN jars present
   - Validate versions if specified

2. **Generate configurations**
   - Create work directories
   - Generate yarn-site.xml, core-site.xml
   - Allocate ports (RM RPC, RM Web, NM RPC, NM Web, etc.)

3. **Start ResourceManager(s)**
   - For each RM: build classpath, start process
   - Wait for RPC server to be ready (ApplicationClientProtocol)
   - Perform health check (getClusterMetrics)
   - If HA enabled, transition RM 0 to ACTIVE

4. **Start NodeManagers**
   - For each NM: build classpath, start process
   - Wait for NM to register with RM
   - Perform health check (verify in NodeReports)

5. **Wait for cluster ready**
   - All NMs registered with RM
   - Cluster out of safe mode
   - Can submit test applications

#### 2.2 Process Health Monitoring

**Health Check Mechanisms:**
- **Process-level**: Check process.isAlive()
- **RPC-level**:
  - RM: Call getClusterMetrics via YarnClient
  - NM: Call getNodeReports via YarnClient
- **Functional**: Submit simple application (e.g., DistributedShell sleep 1)

**Implementation:**
```java
class HealthMonitor {
    boolean checkResourceManagerHealth(InetSocketAddress addr) {
        // Try ApplicationClientProtocol getClusterMetrics()
        // Verify RM is in STARTED state and responding
    }

    boolean checkNodeManagerHealth(InetSocketAddress rmAddr, int nmIndex) {
        // Get NodeReports from RM
        // Check if NM is in RUNNING state
    }
}
```

#### 2.3 Process Shutdown Sequence

1. **Graceful shutdown** (first attempt):
   - Send shutdown command via RMAdminProtocol (for RM)
   - Send shutdown via ResourceTracker or admin (for NM)
   - Wait for process exit (with timeout)

2. **Force shutdown** (if graceful fails):
   - Send SIGTERM to process
   - Wait for exit (with timeout)

3. **Kill** (last resort):
   - Send SIGKILL
   - Clean up resources

4. **Cleanup**:
   - Delete temp directories (optional, or leave for debugging)
   - Close RPC clients
   - Release ports

---

### 3. Dependency Management

#### 3.1 Classpath Isolation Strategy

**Challenge**: Different Hadoop versions have overlapping dependencies but potentially different versions (e.g., Guava, Protobuf, Jetty, Jersey, Log4j).

**Solution: Complete Process Isolation**
- Each node process has fully isolated classpath
- No class sharing except JVM and coordination mechanism
- Communication only via YARN RPC protocols
- Version-specific native libraries via java.library.path

**Classpath Construction:**
```java
List<File> buildClasspathForNode(HadoopDistribution dist, NodeType type) {
    List<File> classpath = new ArrayList<>();

    // 1. Process launcher classes (minimal, from test classpath)
    classpath.add(findProcessLauncherJar());

    // 2. YARN libraries
    classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/yarn"));
    classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/yarn/lib"));

    // 3. Hadoop common libraries
    classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/common"));
    classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/common/lib"));

    // 4. HDFS libraries (YARN depends on HDFS)
    classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/hdfs"));
    classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/hdfs/lib"));

    // 5. MapReduce libraries (for MR application testing)
    if (type == NodeType.RESOURCE_MANAGER) {
        classpath.addAll(findJars(dist.hadoopHome, "share/hadoop/mapreduce"));
    }

    // 6. Native library path
    nativeLibPath = new File(dist.hadoopHome, "lib/native");

    return classpath;
}
```

#### 3.2 Dependency Conflicts

**Known Issues:**
- **Guava**: Hadoop 2.x uses Guava 11.x, Hadoop 3.x uses Guava 27.x
- **Protobuf**: Version incompatibilities between major Hadoop versions
- **Jersey**: Different versions for web services
- **Jetty**: Different versions for web UI
- **Log4j**: Hadoop 2.x uses log4j 1.x, Hadoop 3.x may use log4j 2.x
- **Jackson**: JSON serialization version differences

**Mitigation:**
- Process isolation prevents conflicts
- Each process loads its own dependency versions
- No shared classloader between test JVM and YARN node JVMs

**Testing:**
- Verify different dependency versions work in different nodes
- Test Hadoop 3.3.x RM with Hadoop 3.4.x NMs
- Test cross-version YARN RPC compatibility

---

### 4. Configuration Management

#### 4.1 Port Allocation

**Strategy:**
- Use port range allocation (e.g., 50000-59999)
- Scan for free ports before assignment
- Track assigned ports to avoid conflicts

**Per-Node Ports:**
- **ResourceManager**:
  - RPC port (yarn.resourcemanager.address)
  - Scheduler port (yarn.resourcemanager.scheduler.address)
  - Resource Tracker port (yarn.resourcemanager.resource-tracker.address)
  - Admin port (yarn.resourcemanager.admin.address)
  - Web UI port (yarn.resourcemanager.webapp.address)
- **NodeManager**:
  - Localizer port (yarn.nodemanager.localizer.address)
  - Web UI port (yarn.nodemanager.webapp.address)
  - Collector service port (yarn.nodemanager.collector-service.address)

```java
class PortAllocator {
    private Set<Integer> usedPorts;
    private int nextPort = 50000;

    int allocatePort() {
        while (usedPorts.contains(nextPort) || !isPortAvailable(nextPort)) {
            nextPort++;
        }
        usedPorts.add(nextPort);
        return nextPort++;
    }
}
```

#### 4.2 Directory Structure

```
/tmp/process-miniyarn-<timestamp>/
├── rm0/
│   ├── conf/
│   │   ├── yarn-site.xml
│   │   ├── core-site.xml
│   │   ├── capacity-scheduler.xml
│   │   └── log4j.properties
│   ├── data/
│   │   ├── rm-state-store/
│   │   └── recovery/
│   ├── logs/
│   │   └── yarn-resourcemanager.log
│   └── pid
├── rm1/  (if HA enabled)
│   ├── conf/
│   ├── data/
│   ├── logs/
│   └── pid
├── nm0/
│   ├── conf/
│   ├── local-dirs/
│   │   ├── nm-local-dir/
│   │   └── usercache/
│   ├── log-dirs/
│   │   └── application_*/
│   ├── logs/
│   │   └── yarn-nodemanager.log
│   └── pid
├── nm1/
└── cluster.properties (cluster-wide config)
```

#### 4.3 Configuration File Generation

**Base Configuration Template (yarn-site.xml):**
```xml
<configuration>
  <!-- ResourceManager Configuration -->
  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>localhost</value>
  </property>
  <property>
    <name>yarn.resourcemanager.address</name>
    <value>localhost:${rm_rpc_port}</value>
  </property>
  <property>
    <name>yarn.resourcemanager.scheduler.address</name>
    <value>localhost:${rm_scheduler_port}</value>
  </property>
  <property>
    <name>yarn.resourcemanager.webapp.address</name>
    <value>localhost:${rm_webapp_port}</value>
  </property>

  <!-- NodeManager Configuration -->
  <property>
    <name>yarn.nodemanager.address</name>
    <value>localhost:${nm_rpc_port}</value>
  </property>
  <property>
    <name>yarn.nodemanager.localizer.address</name>
    <value>localhost:${nm_localizer_port}</value>
  </property>
  <property>
    <name>yarn.nodemanager.webapp.address</name>
    <value>localhost:${nm_webapp_port}</value>
  </property>
  <property>
    <name>yarn.nodemanager.local-dirs</name>
    <value>file://${work_dir}/local-dirs</value>
  </property>
  <property>
    <name>yarn.nodemanager.log-dirs</name>
    <value>file://${work_dir}/log-dirs</value>
  </property>

  <!-- Cluster Configuration -->
  <property>
    <name>yarn.scheduler.minimum-allocation-mb</name>
    <value>128</value>
  </property>
  <property>
    <name>yarn.scheduler.maximum-allocation-mb</name>
    <value>2048</value>
  </property>
  <property>
    <name>yarn.nodemanager.resource.memory-mb</name>
    <value>4096</value>
  </property>
  <property>
    <name>yarn.nodemanager.resource.cpu-vcores</name>
    <value>4</value>
  </property>

  <!-- HA Configuration (if enabled) -->
  <property>
    <name>yarn.resourcemanager.ha.enabled</name>
    <value>true</value>
  </property>
  <property>
    <name>yarn.resourcemanager.ha.rm-ids</name>
    <value>rm0,rm1</value>
  </property>
</configuration>
```

**Version-Specific Adjustments:**
- Hadoop 2.x vs 3.x config key differences
- Deprecated property mappings
- Version-specific feature flags (e.g., cgroups, GPU scheduling in newer versions)

---

### 5. API Design

#### 5.1 Builder API

```java
ProcessBasedMiniYARNCluster cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
    .numResourceManagers(2)  // HA mode
    .numNodeManagers(3)
    .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")
    .rmHadoopDistribution(1, "/opt/hadoop-3.3.6")
    .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")
    .nmHadoopDistribution(1, "/opt/hadoop-3.4.0")
    .nmHadoopDistribution(2, "/opt/hadoop-3.3.6")
    .format(true)
    .build();
```

#### 5.2 Supported Operations

**Cluster Management:**
```java
// Startup/shutdown
cluster.start();
cluster.shutdown();
cluster.waitForNodeManagersToConnect(5000);

// Process management
cluster.restartResourceManager(0);
cluster.restartNodeManager(0);
cluster.shutdownResourceManager(0);
cluster.shutdownNodeManager(0);

// Status
cluster.isClusterUp();
cluster.getActiveRMIndex();
cluster.getResourceManagerAddress(0);
cluster.getNodeManagerAddress(0);
```

**Client Operations:**
```java
// Client access
YarnClient yarnClient = cluster.createYarnClient();
Configuration clientConf = cluster.getConfiguration();

// Application submission
ApplicationSubmissionContext appContext = ...;
ApplicationId appId = yarnClient.submitApplication(appContext);

// Monitoring
ApplicationReport report = yarnClient.getApplicationReport(appId);
List<NodeReport> nodes = yarnClient.getNodeReports();
YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
```

#### 5.3 Unsupported Operations

Methods that require direct object access will throw:
```java
throw new UnsupportedOperationException(
    "Direct object access not supported in ProcessBasedMiniYARNCluster. " +
    "This cluster runs nodes in separate processes. " +
    "Use YarnClient APIs instead.");
```

**List of Unsupported Methods:**
- `getResourceManager()` - returns ResourceManager object
- `getResourceManager(int)` - returns ResourceManager object
- `getNodeManager(int)` - returns NodeManager object
- `getApplicationHistoryServer()` - returns AHS object
- Any method returning server-side objects or internal state

---

### 6. Version Upgrade Testing Support

#### 6.1 Upgrade Scenarios

**Supported Test Scenarios:**

1. **Rolling Upgrade - NodeManagers**
   ```java
   // Start with Hadoop 3.3.6
   cluster.allNodesHadoopDistribution("/opt/hadoop-3.3.6");
   cluster.build();

   // Upgrade to Hadoop 3.4.0
   for (int i = 0; i < 3; i++) {
       cluster.shutdownNodeManager(i);
       cluster.changeNodeManagerVersion(i, "/opt/hadoop-3.4.0");
       cluster.startNodeManager(i);
       cluster.waitForNodeManagersToConnect(5000);
       // Verify applications still running
   }
   ```

2. **ResourceManager HA Upgrade**
   ```java
   // Upgrade standby RM to Hadoop 3.4.0
   int standbyIndex = (cluster.getActiveRMIndex() + 1) % 2;
   cluster.shutdownResourceManager(standbyIndex);
   cluster.changeResourceManagerVersion(standbyIndex, "/opt/hadoop-3.4.0");
   cluster.startResourceManager(standbyIndex);

   // Perform failover
   RMAdminCLI rmAdminCLI = new RMAdminCLI(cluster.getConfiguration());
   rmAdminCLI.transitionToActive("rm" + standbyIndex);

   // Upgrade old active
   int oldActiveIndex = cluster.getActiveRMIndex();
   cluster.shutdownResourceManager(oldActiveIndex);
   cluster.changeResourceManagerVersion(oldActiveIndex, "/opt/hadoop-3.4.0");
   cluster.startResourceManager(oldActiveIndex);
   ```

3. **Mixed Version Cluster**
   ```java
   // Different versions on different nodes
   cluster.rmHadoopDistribution(0, "/opt/hadoop-3.3.6");
   cluster.nmHadoopDistribution(0, "/opt/hadoop-3.4.0");
   cluster.nmHadoopDistribution(1, "/opt/hadoop-3.4.0");
   cluster.nmHadoopDistribution(2, "/opt/hadoop-3.3.6");
   cluster.build();
   // Test compatibility matrix
   ```

#### 6.2 Test Helpers

```java
class UpgradeTestHelper {
    void performRollingUpgrade(
        ProcessBasedMiniYARNCluster cluster,
        String fromVersion,
        String toVersion);

    void verifyVersionCompatibility(
        String rmVersion,
        String nmVersion);

    void assertApplicationRuns(YarnClient yarnClient, String appType);

    void submitDistributedShellApp(YarnClient yarnClient, String command);
}
```

---

## Implementation Plan

### Phase 1: Core Infrastructure (Weeks 1-2)

#### Task 1.1: Process Management Foundation
**Priority**: P0
**Estimated Effort**: 3-4 days

**Subtasks:**
- [ ] Create `ProcessNodeManager` base class
  - [ ] Implement start(), stop(), isAlive() methods
  - [ ] Add process monitoring thread
  - [ ] Handle process crashes and restarts
- [ ] Create `ResourceManagerProcessManager`
  - [ ] Implement RM-specific startup
  - [ ] Add RPC health checks (ApplicationClientProtocol)
  - [ ] Support HA mode
- [ ] Create `NodeManagerProcessManager`
  - [ ] Implement NM-specific startup
  - [ ] Add health monitoring (ResourceTracker)
- [ ] Create `ProcessLauncher` base class
  - [ ] Command-line argument parsing
  - [ ] Configuration loading
  - [ ] Logging setup

**Testing:**
- Unit test: Start/stop single RM process
- Unit test: Start/stop single NM process
- Unit test: Process crash detection

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    ├── ProcessNodeManager.java
    ├── ResourceManagerProcessManager.java
    ├── NodeManagerProcessManager.java
    └── launcher/
        ├── ProcessLauncher.java
        ├── ResourceManagerProcessLauncher.java
        └── NodeManagerProcessLauncher.java
```

#### Task 1.2: Classpath & Version Management
**Priority**: P0
**Estimated Effort**: 3-4 days

**Subtasks:**
- [ ] Create `HadoopDistribution` class
  - [ ] Parse Hadoop installation directory
  - [ ] Discover YARN JAR files
  - [ ] Discover Common JAR files
  - [ ] Build classpath string
- [ ] Create `HadoopVersionRegistry`
  - [ ] Register multiple distributions
  - [ ] Validate distribution completeness
- [ ] Implement classpath builder
  - [ ] Collect YARN JARs from share/hadoop/yarn/
  - [ ] Collect dependencies from share/hadoop/yarn/lib/
  - [ ] Collect Common JARs
  - [ ] Handle native libraries
- [ ] Test dependency isolation

**Testing:**
- Unit test: Parse Hadoop distribution
- Unit test: Build classpath for Hadoop 3.3.x
- Unit test: Build classpath for Hadoop 3.4.x
- Integration test: Start RM with Hadoop 3.3.x
- Integration test: Start NM with Hadoop 3.4.x

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    ├── HadoopDistribution.java
    ├── HadoopVersionRegistry.java
    └── ClasspathBuilder.java
```

#### Task 1.3: Configuration Management
**Priority**: P0
**Estimated Effort**: 2-3 days

**Subtasks:**
- [ ] Create `ProcessConfigurationGenerator`
  - [ ] Generate per-node yarn-site.xml files
  - [ ] Generate core-site.xml files
  - [ ] Write config files to disk
- [ ] Create `PortAllocator`
  - [ ] Scan for available ports
  - [ ] Track allocated ports
  - [ ] Handle allocation failures
- [ ] Create `DirectoryManager`
  - [ ] Set up temp directory structure
  - [ ] Create per-node subdirectories (conf, data, logs)
  - [ ] Implement cleanup on shutdown

**Testing:**
- Unit test: Generate RM configuration
- Unit test: Generate NM configuration
- Unit test: Port allocation
- Unit test: Directory structure creation

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    ├── ProcessConfigurationGenerator.java
    ├── PortAllocator.java
    └── DirectoryManager.java
```

### Phase 2: ProcessBasedMiniYARNCluster Implementation (Weeks 3-4)

#### Task 2.1: Main Cluster Class
**Priority**: P0
**Estimated Effort**: 4-5 days

**Subtasks:**
- [ ] Create `ProcessBasedMiniYARNCluster` class
  - [ ] Extend/implement MiniYARNCluster interface
  - [ ] Implement Builder pattern
  - [ ] Add hadoopDistribution() builder methods
- [ ] Implement cluster startup sequence
  - [ ] Validate distributions
  - [ ] Generate configurations
  - [ ] Start RMs in correct order
  - [ ] Start NMs
  - [ ] Wait for cluster ready
- [ ] Implement cluster shutdown sequence
  - [ ] Graceful shutdown
  - [ ] Force shutdown
  - [ ] Cleanup resources
- [ ] Implement supported methods
  - [ ] createYarnClient()
  - [ ] getConfiguration()
  - [ ] waitForNodeManagersToConnect()
  - [ ] restart methods
- [ ] Throw UnsupportedOperationException for direct access methods

**Testing:**
- Integration test: Start single-RM cluster
- Integration test: Start HA cluster (2 RMs)
- Integration test: Restart nodes
- Integration test: Verify unsupported methods throw exceptions

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    ├── ProcessBasedMiniYARNCluster.java
    └── integration/
        ├── TestProcessBasedMiniYARNCluster.java
        └── TestProcessBasedMiniYARNClusterAPI.java
```

#### Task 2.2: Health Monitoring & Retry Logic
**Priority**: P1
**Estimated Effort**: 2-3 days

**Subtasks:**
- [ ] Create `HealthMonitor` class
  - [ ] Check process alive status
  - [ ] Verify RPC connectivity (YarnClient.getClusterMetrics)
  - [ ] Functional health checks (submit test app)
- [ ] Implement waitForNodeReady()
  - [ ] Wait for RPC server
  - [ ] Retry with exponential backoff
  - [ ] Timeout handling
- [ ] Implement cluster readiness checks
  - [ ] All NMs healthy
  - [ ] RM out of safe mode
  - [ ] Can submit applications

**Testing:**
- Unit test: Health check for healthy RM
- Unit test: Health check for dead RM
- Integration test: Wait for cluster ready
- Integration test: Handle node startup failures

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    └── HealthMonitor.java
```

### Phase 3: Multi-Version Support (Week 5)

#### Task 3.1: Version-Specific Configuration
**Priority**: P1
**Estimated Effort**: 2-3 days

**Subtasks:**
- [ ] Create `VersionConfigAdapter`
  - [ ] Map config keys between Hadoop versions
  - [ ] Handle deprecated properties
  - [ ] Version-specific defaults
- [ ] Test version compatibility
  - [ ] Hadoop 3.3.x <-> Hadoop 3.4.x property mapping
  - [ ] Handle removed/renamed properties
- [ ] Test minor version compatibility

**Testing:**
- Unit test: Config key mapping
- Integration test: Mixed-version cluster (3.3.x RM with 3.4.x NMs)

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    └── VersionConfigAdapter.java
```

#### Task 3.2: Mixed-Version Cluster Testing
**Priority**: P1
**Estimated Effort**: 2-3 days

**Subtasks:**
- [ ] Create test matrix of version combinations
  - [ ] Document supported combinations
  - [ ] Test known compatible versions
  - [ ] Identify incompatible combinations
- [ ] Implement version upgrade test helpers
  - [ ] Rolling upgrade helper
  - [ ] RM HA upgrade helper
  - [ ] Compatibility verification
- [ ] Create example upgrade tests

**Testing:**
- Integration test: All nodes different versions
- Integration test: Rolling upgrade NMs
- Integration test: RM HA upgrade
- Integration test: Version incompatibility detection

**Files to Create:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/upgrade/
    ├── TestMixedVersionCluster.java
    ├── TestRollingUpgrade.java
    ├── TestRMHAUpgrade.java
    └── UpgradeTestHelper.java
```

### Phase 4: Testing & Documentation (Week 6)

#### Task 4.1: Comprehensive Test Suite
**Priority**: P0
**Estimated Effort**: 3-4 days

**Subtasks:**
- [ ] Unit tests for all components
- [ ] Integration tests
- [ ] Stress tests
- [ ] Compatibility tests

**Test Categories:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/
└── src/test/java/org/apache/hadoop/yarn/server/process/
    ├── unit/
    │   ├── TestProcessNodeManager.java
    │   ├── TestHadoopVersionRegistry.java
    │   ├── TestConfigurationGenerator.java
    │   └── TestPortAllocator.java
    ├── integration/
    │   ├── TestProcessBasedMiniYARNClusterBasics.java
    │   ├── TestProcessBasedMiniYARNClusterFailover.java
    │   ├── TestProcessBasedMiniYARNClusterUpgrade.java
    │   └── TestMixedVersionOperations.java
    └── compatibility/
        └── TestAPICompatibility.java
```

#### Task 4.2: Documentation
**Priority**: P1
**Estimated Effort**: 2 days

**Subtasks:**
- [ ] Create user guide
- [ ] Create developer guide
- [ ] Update YARN testing documentation
- [ ] Create JavaDoc

**Documentation Files:**
```
hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests/docs/
├── ProcessBasedMiniYARNCluster-UserGuide.md
├── ProcessBasedMiniYARNCluster-DeveloperGuide.md
└── VersionUpgradeTestingGuide.md
```

---

## Testing Strategy

### Unit Testing

**Coverage Goals**: >80% line coverage for all new classes

**Test Categories:**

1. **Process Management**
   - Start/stop RM and NM processes
   - Process crash detection
   - Graceful vs force shutdown
   - PID tracking

2. **Configuration**
   - yarn-site.xml generation
   - Port allocation
   - Directory management
   - Version-specific config

3. **Classpath**
   - JAR discovery in Hadoop distribution
   - Classpath construction
   - Dependency isolation

4. **Health Monitoring**
   - RPC connectivity checks
   - Process health checks
   - Retry logic

### Integration Testing

**Test Environments:**
- Single RM, single NM cluster
- HA cluster (2 RMs, 3 NMs)
- Mixed-version cluster

**Test Scenarios:**

1. **Basic Operations**
   ```java
   @Test
   public void testBasicApplicationSubmission() {
       cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
           .numResourceManagers(1)
           .numNodeManagers(3)
           .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
           .build();

       YarnClient yarnClient = cluster.createYarnClient();

       // Submit DistributedShell application
       ApplicationId appId = submitDistributedShellApp(yarnClient, "sleep 5");

       // Wait for completion
       waitForAppCompletion(yarnClient, appId);

       // Verify success
       ApplicationReport report = yarnClient.getApplicationReport(appId);
       assertEquals(FinalApplicationStatus.SUCCEEDED, report.getFinalApplicationStatus());
   }
   ```

2. **Node Restart**
   ```java
   @Test
   public void testNodeManagerRestart() {
       // Submit application
       ApplicationId appId = submitDistributedShellApp(yarnClient, "sleep 60");

       // Restart NM while app is running
       cluster.restartNodeManager(0);
       cluster.waitForNodeManagersToConnect(5000);

       // Verify app continues or gets rescheduled
       ApplicationReport report = yarnClient.getApplicationReport(appId);
       assertNotEquals(YarnApplicationState.FAILED, report.getYarnApplicationState());
   }
   ```

3. **Mixed Versions**
   ```java
   @Test
   public void testMixedVersionCluster() {
       cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
           .numNodeManagers(3)
           .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")
           .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")
           .nmHadoopDistribution(1, "/opt/hadoop-3.4.0")
           .nmHadoopDistribution(2, "/opt/hadoop-3.3.6")
           .build();

       // Verify all nodes work together
       YarnClient yarnClient = cluster.createYarnClient();
       List<NodeReport> nodes = yarnClient.getNodeReports();
       assertEquals(3, nodes.size());

       // Submit and run application
       ApplicationId appId = submitDistributedShellApp(yarnClient, "echo test");
       waitForAppCompletion(yarnClient, appId);
   }
   ```

4. **Rolling Upgrade**
   ```java
   @Test
   public void testRollingUpgradeNodeManagers() {
       // Start cluster with Hadoop 3.3.6
       cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
           .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
           .numNodeManagers(3)
           .build();

       // Submit long-running application
       ApplicationId appId = submitDistributedShellApp(yarnClient, "sleep 300");

       // Upgrade each NM one by one
       for (int i = 0; i < 3; i++) {
           cluster.shutdownNodeManager(i);
           cluster.changeNodeManagerVersion(i, "/opt/hadoop-3.4.0");
           cluster.startNodeManager(i);
           cluster.waitForNodeManagersToConnect(5000);

           // Verify app still running or rescheduled
           ApplicationReport report = yarnClient.getApplicationReport(appId);
           assertNotEquals(FinalApplicationStatus.FAILED,
               report.getFinalApplicationStatus());
       }

       // Verify all NMs upgraded
       List<NodeReport> nodes = yarnClient.getNodeReports();
       assertEquals(3, nodes.size());
   }
   ```

---

## Dependencies & Prerequisites

### Runtime Dependencies

1. **Multiple Hadoop Distributions**
   - User must provide built Hadoop installations
   - Suggested setup: `/opt/hadoop-3.3.6/`, `/opt/hadoop-3.4.0/`, etc.
   - Each installation must be complete (jars + dependencies + native libs)

2. **JDK 8 or 11**
   - Same JDK for all versions recommended
   - Hadoop 3.3+ requires JDK 8 or 11

3. **Sufficient Resources**
   - Each RM process: ~512MB heap
   - Each NM process: ~512MB heap + container memory
   - For 1 RM + 3 NM cluster: ~3-4GB total

4. **Operating System**
   - Linux: Fully supported
   - macOS: Should work (test needed)
   - Windows: May have issues (lower priority)

### Build Dependencies

- Maven 3.3+
- JUnit 4 or JUnit 5 for parameterized tests
- Mockito for unit tests
- Hadoop dependencies (automatically resolved)

---

## Risk Assessment & Mitigation

### High Risk Items

#### Risk 1: Dependency Hell
**Description**: Different Hadoop versions have conflicting dependencies (Guava, Protobuf, etc.)

**Impact**: High - Could prevent different versions from running together

**Probability**: Medium

**Mitigation**:
- Complete process isolation (separate JVMs)
- No shared classpath except JVM itself
- Extensive testing of known problematic dependencies

**Contingency**:
- If isolation fails, use Docker containers instead
- Fall back to version ranges (only test compatible versions)

#### Risk 2: YARN Protocol Incompatibility
**Description**: YARN RPC protocols differ between major versions

**Impact**: High - Nodes can't communicate across versions

**Probability**: Low (Hadoop maintains backward compatibility)

**Mitigation**:
- Test with known compatible versions first (3.3.x <-> 3.4.x)
- Document minimum compatible versions
- Check Hadoop compatibility matrix

**Contingency**:
- Limit support to compatible version ranges
- Provide clear error messages for incompatible versions

#### Risk 3: Configuration Incompatibility
**Description**: Config keys/values differ between Hadoop versions

**Impact**: High - Nodes won't start with wrong config

**Probability**: Medium

**Mitigation**:
- Version-aware config generation (VersionConfigAdapter)
- Test with actual distributions
- Document known incompatibilities

**Contingency**:
- Provide manual config override mechanism
- Version-specific config templates

---

## Success Criteria

### Phase 1 Success Criteria
- [ ] Can start single RM process with specific Hadoop version
- [ ] Can start single NM process with specific Hadoop version
- [ ] Process monitoring and health checks work
- [ ] Proper cleanup on shutdown

### Phase 2 Success Criteria
- [ ] Can start full YARN cluster (1 RM + 3 NMs)
- [ ] Can submit and run applications via YarnClient
- [ ] Can restart nodes without cluster restart
- [ ] Unsupported methods throw clear exceptions

### Phase 3 Success Criteria
- [ ] Can run different nodes with different Hadoop versions
- [ ] Can perform rolling upgrade of NodeManagers
- [ ] Can perform RM HA upgrade
- [ ] Version compatibility matrix documented and tested

### Final Success Criteria
- [ ] All unit tests pass (>80% coverage)
- [ ] All integration tests pass
- [ ] At least 5 version combination tests pass
- [ ] Documentation complete
- [ ] Zero known critical bugs
- [ ] Can run at least one rolling upgrade scenario end-to-end

---

## Timeline

**Total Estimated Duration**: 6 weeks

| Phase | Duration | End Date | Deliverables |
|-------|----------|----------|--------------|
| Phase 1: Core Infrastructure | 2 weeks | Week 2 | Process management, classpath isolation, config generation |
| Phase 2: Main Implementation | 2 weeks | Week 4 | ProcessBasedMiniYARNCluster class, basic functionality |
| Phase 3: Multi-Version Support | 1 week | Week 5 | Version adapter, upgrade helpers |
| Phase 4: Testing & Documentation | 1 week | Week 6 | Comprehensive tests, documentation |

**Milestones:**
- Week 2: First RM process started successfully
- Week 3: First application submitted and completed
- Week 4: First full cluster test passes
- Week 5: First mixed-version test passes, first rolling upgrade works
- Week 6: Ready for code review

---

## Example Usage

```java
// Basic usage - all nodes same Hadoop version
Configuration conf = new YarnConfiguration();
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
        .format(true)
        .build();

YarnClient yarnClient = cluster.createYarnClient();
// Submit applications, run tests...
cluster.shutdown();

// Mixed version usage
ProcessBasedMiniYARNCluster mixedCluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numNodeManagers(3)
        .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")
        .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")
        .nmHadoopDistribution(1, "/opt/hadoop-3.4.0")
        .nmHadoopDistribution(2, "/opt/hadoop-3.3.6")
        .format(true)
        .build();

// HA cluster with upgrade scenario
ProcessBasedMiniYARNCluster haCluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)  // HA
        .numNodeManagers(3)
        .allNodesHadoopDistribution("/opt/hadoop-3.3.6")
        .build();

// Upgrade standby RM
int standbyIndex = (haCluster.getActiveRMIndex() + 1) % 2;
haCluster.shutdownResourceManager(standbyIndex);
haCluster.changeResourceManagerVersion(standbyIndex, "/opt/hadoop-3.4.0");
haCluster.startResourceManager(standbyIndex);

// Perform failover
// ... (use RMAdminCLI or admin protocol)

// Upgrade old active
// ... (similar to standby upgrade)
```

---

**End of Implementation Guide**

This guide provides a comprehensive roadmap for implementing ProcessBasedMiniYARNCluster for Apache Hadoop YARN. Follow the phases sequentially, track progress with the provided checklists, and refer to the detailed sections for specific implementation guidance.
