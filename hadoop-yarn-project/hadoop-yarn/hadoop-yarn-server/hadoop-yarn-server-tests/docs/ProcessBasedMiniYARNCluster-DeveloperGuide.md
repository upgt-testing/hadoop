# ProcessBasedMiniYARNCluster Developer Guide

**Version**: 1.0
**Last Updated**: 2025-11-09
**Target Audience**: YARN developers extending or modifying ProcessBasedMiniYARNCluster

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Design](#component-design)
3. [Code Organization](#code-organization)
4. [Design Patterns](#design-patterns)
5. [Extending the Framework](#extending-the-framework)
6. [Adding New Features](#adding-new-features)
7. [Testing Guidelines](#testing-guidelines)
8. [Debugging](#debugging)
9. [Performance Considerations](#performance-considerations)
10. [Contribution Guidelines](#contribution-guidelines)

---

## Architecture Overview

### Core Principles

1. **Process Isolation**: Each YARN node runs in a separate JVM process with isolated classpath
2. **RPC-Only Communication**: Test code interacts with cluster via YarnClient APIs only (no direct object access)
3. **Multi-Version Support**: Different nodes can run different Hadoop versions simultaneously
4. **Deterministic Configuration**: All configuration generated programmatically, no external dependencies
5. **Resource Cleanup**: Automatic cleanup of processes, ports, and temporary directories

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Test JVM Process                         │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │   ProcessBasedMiniYARNCluster (Coordinator)         │  │
│  ├─────────────────────────────────────────────────────┤  │
│  │  - HadoopVersionRegistry                            │  │
│  │  - DirectoryManager                                 │  │
│  │  - PortAllocator                                    │  │
│  │  - ProcessConfigurationGenerator                    │  │
│  │  - ProcessNodeManager[] (RM + NM instances)         │  │
│  └─────────────────────────────────────────────────────┘  │
│                      │                                      │
│                      │ Process.spawn()                      │
│                      ▼                                      │
└─────────────────────────────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌──────────┐   ┌──────────┐   ┌──────────┐
  │ RM JVM 0 │   │ RM JVM 1 │   │ NM JVM 0 │
  ├──────────┤   ├──────────┤   ├──────────┤
  │ Launcher │   │ Launcher │   │ Launcher │
  │    ↓     │   │    ↓     │   │    ↓     │
  │   RM     │   │   RM     │   │   NM     │
  └──────────┘   └──────────┘   └──────────┘
```

### Component Relationships

```
ProcessBasedMiniYARNCluster
│
├── HadoopVersionRegistry
│   ├── HadoopDistribution (3.3.6)
│   └── HadoopDistribution (3.4.0)
│
├── DirectoryManager
│   ├── ClusterRoot (/tmp/process-miniyarn-*)
│   ├── RM Directories (rm0/, rm1/)
│   └── NM Directories (nm0/, nm1/, nm2/)
│
├── PortAllocator
│   └── Allocated Ports Set (50000-59999)
│
├── ProcessConfigurationGenerator
│   ├── generateResourceManagerConfig()
│   └── generateNodeManagerConfig()
│
├── ResourceManagerProcessManager[]
│   ├── ProcessLauncher → ResourceManagerProcessLauncher
│   ├── Process (Java subprocess)
│   └── HealthMonitor
│
└── NodeManagerProcessManager[]
    ├── ProcessLauncher → NodeManagerProcessLauncher
    ├── Process (Java subprocess)
    └── HealthMonitor
```

---

## Component Design

### 1. ProcessNodeManager (Abstract Base Class)

**Purpose**: Base class for managing individual YARN node processes (RM or NM).

**Responsibilities**:
- Subprocess lifecycle (start, stop, restart)
- Classpath construction from HadoopDistribution
- Health monitoring (process liveness + RPC connectivity)
- Output streaming (stdout/stderr readers)
- Graceful shutdown (SIGTERM → SIGKILL escalation)

**Key Methods**:
```java
protected abstract void start() throws IOException;
protected abstract void stop() throws IOException;
protected abstract boolean isHealthy() throws IOException;

protected List<String> buildClasspath() throws IOException;
protected void waitForProcessReady(long timeoutMs);
protected boolean isProcessAlive();
```

**Design Pattern**: **Template Method** - Subclasses override start()/stop()/isHealthy() with node-specific logic.

### 2. ResourceManagerProcessManager / NodeManagerProcessManager

**Purpose**: RM-specific and NM-specific process management.

**ResourceManagerProcessManager**:
- Launches ResourceManagerProcessLauncher subprocess
- Health checks via socket connectivity to RPC port
- Supports HA mode (rmId parameter)
- Monitors state store directory

**NodeManagerProcessManager**:
- Launches NodeManagerProcessLauncher subprocess
- Health checks via socket connectivity to RPC port
- Monitors local-dirs and log-dirs

**Key Differences from MiniYARNCluster**:
- No direct RM/NM object access
- Socket-based health checks instead of YarnClient RPC (avoids cyclic dependency)
- Each node has isolated classpath

### 3. ProcessLauncher (Subprocess Entry Point)

**Purpose**: Main class for YARN node subprocesses.

**Responsibilities**:
- Parse command-line arguments
- Load configuration from files
- Start RM or NM
- Set up shutdown hooks

**Subclasses**:
- **ResourceManagerProcessLauncher**: Creates and starts ResourceManager
- **NodeManagerProcessLauncher**: Creates and starts NodeManager

**Classpath Isolation**:
```bash
java -cp /opt/hadoop-3.3.6/share/hadoop/yarn/*:... \
  org.apache.hadoop.yarn.server.process.launcher.ResourceManagerProcessLauncher \
  --rmIndex 0 \
  --workDir /tmp/process-miniyarn-123/rm0
```

### 4. HadoopDistribution

**Purpose**: Represents a Hadoop installation on disk.

**Responsibilities**:
- Validate Hadoop distribution completeness
- Discover JAR files (share/hadoop/{yarn,common,hdfs,mapreduce})
- Build classpath strings
- Auto-detect Hadoop version from JAR filenames

**Validation Logic**:
```java
public void validate() throws IOException {
    File hadoopHomeDir = new File(hadoopHome);
    if (!hadoopHomeDir.exists()) {
        throw new IOException("Hadoop home does not exist: " + hadoopHome);
    }

    File shareDir = new File(hadoopHome, "share/hadoop");
    if (!shareDir.exists()) {
        throw new IOException("share/hadoop directory not found");
    }

    // Verify critical subdirectories exist
    verifySubdirectory(shareDir, "common");
    verifySubdirectory(shareDir, "yarn");
}
```

### 5. HadoopVersionRegistry

**Purpose**: Thread-safe registry for managing multiple Hadoop distributions.

**Design Pattern**: **Registry** - Centralized lookup for Hadoop distributions by version or alias.

**Thread Safety**: ConcurrentHashMap for concurrent access.

**System Property Integration**:
```java
public static HadoopVersionRegistry fromSystemProperties() {
    HadoopVersionRegistry registry = new HadoopVersionRegistry();

    String startHome = System.getProperty("hadoop.start.home");
    if (startHome != null) {
        registry.register("start-version", startHome);
    }

    String upgradeHome = System.getProperty("hadoop.upgrade.home");
    if (upgradeHome != null) {
        registry.register("upgrade-version", upgradeHome);
    }

    return registry;
}
```

### 6. PortAllocator

**Purpose**: Dynamic port allocation to avoid conflicts.

**Algorithm**:
1. Scan from start port (50000) to end port (59999)
2. Attempt to bind ServerSocket to port
3. If successful, port is available → allocate and track
4. If failure, try next port
5. Throw IOException if all ports exhausted

**Thread Safety**: Synchronized methods for concurrent allocation.

**Port Range**: 50000-59999 (user ports, less likely to conflict).

### 7. DirectoryManager

**Purpose**: Create and manage temporary directory structure for cluster.

**Directory Layout**:
```
/tmp/process-miniyarn-<timestamp>/
├── rm0/
│   ├── conf/          (yarn-site.xml, core-site.xml)
│   ├── data/          (RM state)
│   │   └── rm-state-store/
│   ├── logs/          (stdout.log, stderr.log)
│   └── pid            (Process ID file)
├── rm1/
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

**Cleanup Strategy**:
- `deleteOnCleanup = true` (default): Delete on cleanup()
- `deleteOnCleanup = false`: Keep for debugging

### 8. ProcessConfigurationGenerator

**Purpose**: Generate yarn-site.xml and core-site.xml for each node.

**Configuration Generation Flow**:
```
Base YarnConfiguration (from user)
         ↓
    Copy to node config
         ↓
    Set node-specific properties:
    - RPC addresses (allocated ports)
    - Work directories (from DirectoryManager)
    - HA configuration (if enabled)
    - State store directories
         ↓
    Write to XML files in node conf directory
```

**HA Configuration Logic**:
```java
if (haEnabled) {
    config.setBoolean(YarnConfiguration.RM_HA_ENABLED, true);
    config.set(YarnConfiguration.RM_HA_IDS, String.join(",", rmIds));

    for (int i = 0; i < rmIds.length; i++) {
        String rmId = rmIds[i];
        config.set(YarnConfiguration.RM_HOSTNAME + "." + rmId, "localhost");
        config.set(YarnConfiguration.RM_ADDRESS + "." + rmId,
            "localhost:" + rmRpcPorts[i]);
        // ... more HA properties
    }
}
```

### 9. HealthMonitor

**Purpose**: Health checking and retry logic with exponential backoff.

**Design Pattern**: **Strategy** - Different health check strategies (socket, RPC, custom predicate).

**Retry Logic**:
```java
public <T> T retryWithBackoff(Callable<T> task, int maxAttempts, String description) {
    long delay = initialRetryDelayMs;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return task.call();
        } catch (Exception e) {
            if (attempt == maxAttempts) {
                throw new IOException("Failed after " + maxAttempts + " attempts", e);
            }

            Thread.sleep(delay);
            delay = Math.min((long)(delay * retryBackoffMultiplier), maxRetryDelayMs);
        }
    }
}
```

**Socket-Based Health Checks** (avoids YarnClient dependency):
```java
public boolean isRpcReachable(InetSocketAddress rpcAddress) {
    try (Socket socket = new Socket()) {
        socket.connect(rpcAddress, 1000);  // 1s timeout
        return true;
    } catch (IOException e) {
        return false;
    }
}
```

### 10. VersionConfigAdapter

**Purpose**: Adapt configurations between different Hadoop versions.

**Design Pattern**: **Adapter** - Translate config keys between Hadoop versions.

**Version Detection**:
```java
private static String getMajorMinorVersion(String version) {
    String[] parts = version.split("\\.");
    if (parts.length < 2) {
        return version;
    }
    return parts[0] + "." + parts[1];  // e.g., "3.3" from "3.3.6"
}
```

**Compatibility Logic**:
```java
public static boolean areCompatible(String version1, String version2) {
    String majorMinor1 = getMajorMinorVersion(version1);
    String majorMinor2 = getMajorMinorVersion(version2);
    return majorMinor1.equals(majorMinor2);
}
```

---

## Code Organization

### Package Structure

```
org.apache.hadoop.yarn.server.process
│
├── ProcessBasedMiniYARNCluster.java         (Main cluster class)
├── ProcessNodeManager.java                   (Abstract base for process mgmt)
├── ResourceManagerProcessManager.java        (RM-specific process mgmt)
├── NodeManagerProcessManager.java            (NM-specific process mgmt)
├── HadoopDistribution.java                   (Hadoop installation parser)
├── HadoopVersionRegistry.java                (Distribution registry)
├── PortAllocator.java                        (Dynamic port allocation)
├── DirectoryManager.java                     (Temp directory management)
├── ProcessConfigurationGenerator.java        (Config XML generation)
├── HealthMonitor.java                        (Health checks + retry logic)
├── VersionConfigAdapter.java                 (Version compatibility)
│
├── launcher/
│   ├── ProcessLauncher.java                  (Subprocess entry point base)
│   ├── ResourceManagerProcessLauncher.java   (RM subprocess main)
│   └── NodeManagerProcessLauncher.java       (NM subprocess main)
│
└── upgrade/
    ├── UpgradeTestHelper.java                (Upgrade orchestration)
    └── VERSION_COMPATIBILITY.md              (Compatibility guide)
```

### Test Organization

```
org.apache.hadoop.yarn.server.process
│
├── unit/
│   ├── TestPortAllocator.java
│   ├── TestDirectoryManager.java
│   ├── TestHadoopDistribution.java
│   ├── TestHadoopVersionRegistry.java
│   ├── TestHealthMonitor.java
│   └── TestVersionConfigAdapter.java
│
├── integration/
│   ├── TestProcessBasedMiniYARNClusterBasics.java
│   ├── TestProcessBasedMiniYARNClusterFailover.java
│   └── TestProcessBasedMiniYARNClusterUpgrade.java
│
└── compatibility/
    └── TestAPICompatibility.java
```

---

## Design Patterns

### 1. Builder Pattern (ProcessBasedMiniYARNCluster)

**Why**: Complex object construction with many optional parameters.

```java
ProcessBasedMiniYARNCluster cluster =
    new ProcessBasedMiniYARNCluster.Builder(conf)
        .numResourceManagers(2)
        .numNodeManagers(3)
        .rmHadoopDistribution(0, "/opt/hadoop-3.3.6")
        .nmHadoopDistribution(0, "/opt/hadoop-3.4.0")
        .build();
```

**Benefits**:
- Fluent API
- Validation at build() time
- Immutable cluster once built

### 2. Template Method (ProcessNodeManager)

**Why**: Common process management logic with node-specific variations.

```java
public abstract class ProcessNodeManager {
    // Template method
    protected void waitForProcessReady(long timeoutMs) {
        // Common waiting logic
        GenericTestUtils.waitFor(() -> isHealthy(), 1000, timeoutMs);
    }

    // Abstract methods for subclasses
    protected abstract void start() throws IOException;
    protected abstract boolean isHealthy() throws IOException;
}
```

**Benefits**:
- Code reuse for common logic
- Extensibility for new node types

### 3. Strategy Pattern (HealthMonitor)

**Why**: Flexible health checking strategies.

```java
// Strategy 1: Socket-based RPC check
monitor.waitForRpcReady(address, timeout);

// Strategy 2: Custom predicate
monitor.waitFor(() -> myCustomHealthCheck(), timeout, "custom check");

// Strategy 3: Retry with backoff
monitor.retryWithBackoff(() -> myTask(), maxAttempts, "task");
```

### 4. Registry Pattern (HadoopVersionRegistry)

**Why**: Centralized lookup for Hadoop distributions.

```java
HadoopVersionRegistry registry = new HadoopVersionRegistry();
registry.register("3.3.6", "/opt/hadoop-3.3.6");
registry.register("start-version", "/opt/hadoop-3.3.6");  // Alias

HadoopDistribution dist = registry.get("3.3.6");
HadoopDistribution sameDist = registry.get("start-version");
```

### 5. Adapter Pattern (VersionConfigAdapter)

**Why**: Translate configurations between incompatible versions.

```java
VersionConfigAdapter adapter = VersionConfigAdapter.forVersion("3.4.0");
YarnConfiguration adapted = adapter.adapt(config336, "3.3.6");
// config336 keys → 3.4.0 keys
```

---

## Extending the Framework

### Adding Support for a New Node Type

**Example: Adding HistoryServer support**

1. **Create Process Manager**:
```java
public class HistoryServerProcessManager extends ProcessNodeManager {
    @Override
    protected void start() throws IOException {
        // Build command: java -cp ... HistoryServerProcessLauncher ...
        List<String> command = new ArrayList<>();
        command.add(getJavaExecutable());
        command.add("-cp");
        command.add(hadoopDistribution.buildClasspathString());
        command.add("org.apache.hadoop.yarn.server.process.launcher.HistoryServerProcessLauncher");
        command.add("--workDir");
        command.add(workDirectory.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        process = pb.start();

        // Start output readers
        startOutputReaders();
    }

    @Override
    protected boolean isHealthy() throws IOException {
        if (!isProcessAlive()) {
            return false;
        }

        // Check HistoryServer RPC port
        InetSocketAddress rpcAddress = ...;
        try (Socket socket = new Socket()) {
            socket.connect(rpcAddress, 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
```

2. **Create Process Launcher**:
```java
public class HistoryServerProcessLauncher extends ProcessLauncher {
    public static void main(String[] args) throws Exception {
        HistoryServerProcessLauncher launcher = new HistoryServerProcessLauncher();
        launcher.parseArgs(args);

        YarnConfiguration conf = launcher.loadConfiguration();
        JobHistoryServer historyServer = new JobHistoryServer();
        historyServer.init(conf);
        historyServer.start();

        // Wait for shutdown
        synchronized (launcher) {
            launcher.wait();
        }
    }
}
```

3. **Extend ProcessBasedMiniYARNCluster**:
```java
public class ProcessBasedMiniYARNCluster {
    private List<HistoryServerProcessManager> historyServers;

    public static class Builder {
        private int numHistoryServers = 0;

        public Builder numHistoryServers(int num) {
            this.numHistoryServers = num;
            return this;
        }
    }

    private void startHistoryServers() throws IOException, TimeoutException {
        for (int i = 0; i < numHistoryServers; i++) {
            HistoryServerProcessManager hs = new HistoryServerProcessManager(...);
            hs.start();
            historyServers.add(hs);
        }
    }
}
```

### Adding a New Version Mapping

**Example: Adding 3.5.0 support**

```java
public class VersionConfigAdapter {
    private void buildKeyMappings() {
        // Existing mappings...

        // New 3.5.0 mappings
        if (targetMajorMinor.equals("3.5")) {
            // Map deprecated keys in 3.5.0
            keyMappings.put("yarn.old.key", "yarn.new.key");

            // Mark removed keys
            deprecatedKeys.add("yarn.removed.in.3.5");
        }
    }

    private void applyVersionDefaults(Configuration config) {
        // Existing defaults...

        if (targetMajorMinor.equals("3.5")) {
            // 3.5.0-specific defaults
            config.setIfUnset("yarn.new.feature.enabled", "true");
        }
    }
}
```

### Adding Custom Health Checks

```java
public class CustomHealthMonitor extends HealthMonitor {
    public boolean isApplicationHealthy(YarnClient client, ApplicationId appId) {
        try {
            ApplicationReport report = client.getApplicationReport(appId);
            YarnApplicationState state = report.getYarnApplicationState();
            return state == YarnApplicationState.RUNNING;
        } catch (Exception e) {
            return false;
        }
    }

    public void waitForApplicationRunning(YarnClient client,
                                         ApplicationId appId,
                                         long timeoutMs)
            throws TimeoutException, InterruptedException {
        waitFor(() -> isApplicationHealthy(client, appId),
               timeoutMs,
               "Application " + appId + " to reach RUNNING");
    }
}
```

---

## Adding New Features

### Feature: Cluster Snapshots

**Use Case**: Save cluster state for debugging or replay.

```java
public class ClusterSnapshot {
    private final Map<String, YarnConfiguration> nodeConfigs;
    private final Map<String, String> nodeVersions;
    private final File snapshotDir;

    public static ClusterSnapshot take(ProcessBasedMiniYARNCluster cluster) {
        // Serialize configurations
        Map<String, YarnConfiguration> configs = new HashMap<>();
        for (int i = 0; i < cluster.getNumResourceManagers(); i++) {
            configs.put("rm" + i, cluster.getResourceManagerConfiguration(i));
        }
        for (int i = 0; i < cluster.getNumNodeManagers(); i++) {
            configs.put("nm" + i, cluster.getNodeManagerConfiguration(i));
        }

        // Copy log files
        File snapshotDir = new File("/tmp/cluster-snapshot-" +
            System.currentTimeMillis());
        FileUtils.copyDirectory(cluster.getClusterRoot(), snapshotDir);

        return new ClusterSnapshot(configs, versions, snapshotDir);
    }

    public void restore(ProcessBasedMiniYARNCluster.Builder builder) {
        // Apply saved configurations to builder
        for (Map.Entry<String, YarnConfiguration> entry : nodeConfigs.entrySet()) {
            // ... apply configs
        }
    }
}
```

### Feature: Metrics Collection

**Use Case**: Track performance metrics across upgrades.

```java
public class ClusterMetricsCollector {
    private final ProcessBasedMiniYARNCluster cluster;
    private final List<MetricSnapshot> snapshots = new ArrayList<>();

    public void collectSnapshot() throws IOException {
        YarnClient client = YarnClient.createYarnClient();
        client.init(cluster.getConfiguration());
        client.start();

        try {
            YarnClusterMetrics metrics = client.getYarnClusterMetrics();
            List<NodeReport> nodes = client.getNodeReports();

            MetricSnapshot snapshot = new MetricSnapshot(
                System.currentTimeMillis(),
                metrics.getNumNodeManagers(),
                metrics.getNumActiveNodeManagers(),
                nodes.stream().mapToInt(n -> n.getCapability().getMemory()).sum()
            );

            snapshots.add(snapshot);
        } finally {
            client.stop();
        }
    }

    public void dumpMetrics(File outputFile) throws IOException {
        // Write metrics to CSV or JSON
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.println("timestamp,numNMs,activeNMs,totalMemoryMB");
            for (MetricSnapshot snapshot : snapshots) {
                writer.println(snapshot.toCsv());
            }
        }
    }
}
```

---

## Testing Guidelines

### Unit Test Structure

```java
public class TestMyComponent {
    private MyComponent component;
    private File testDir;

    @Before
    public void setUp() {
        // Create test fixtures
        testDir = new File(System.getProperty("java.io.tmpdir"),
            "test-" + System.currentTimeMillis());
        testDir.mkdirs();

        component = new MyComponent();
    }

    @After
    public void tearDown() {
        // Cleanup resources
        if (testDir != null && testDir.exists()) {
            FileUtils.deleteQuietly(testDir);
        }

        if (component != null) {
            component.cleanup();
        }
    }

    @Test
    public void testBasicOperation() {
        // Test implementation
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInput() {
        component.doSomething(null);  // Should throw
    }

    @Test
    public void testConcurrency() throws Exception {
        // Thread safety test
        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    component.doSomething();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
    }
}
```

### Integration Test Structure

```java
public class TestProcessBasedMiniYARNClusterBasics {
    private ProcessBasedMiniYARNCluster cluster;
    private YarnClient yarnClient;

    @Before
    public void setUp() throws Exception {
        YarnConfiguration conf = new YarnConfiguration();

        cluster = new ProcessBasedMiniYARNCluster.Builder(conf)
            .numResourceManagers(1)
            .numNodeManagers(1)
            .build();

        cluster.start();

        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(cluster.getConfiguration());
        yarnClient.start();
    }

    @After
    public void tearDown() throws Exception {
        if (yarnClient != null) {
            yarnClient.stop();
        }

        if (cluster != null) {
            cluster.shutdown();
        }
    }

    @Test
    public void testClusterStartup() throws Exception {
        // Wait for NM registration
        GenericTestUtils.waitFor(() -> {
            try {
                return yarnClient.getYarnClusterMetrics().getNumNodeManagers() == 1;
            } catch (Exception e) {
                return false;
            }
        }, 1000, 30000);

        // Verify cluster state
        YarnClusterMetrics metrics = yarnClient.getYarnClusterMetrics();
        Assert.assertEquals(1, metrics.getNumNodeManagers());
    }
}
```

### Test Best Practices

1. **Always Use try-finally for Cleanup**:
   ```java
   ProcessBasedMiniYARNCluster cluster = null;
   try {
       cluster = builder.build();
       cluster.start();
       // Test code
   } finally {
       if (cluster != null) {
           cluster.shutdown();
       }
   }
   ```

2. **Wait for Asynchronous Operations**:
   ```java
   // ❌ BAD - Race condition
   cluster.start();
   int numNMs = yarnClient.getYarnClusterMetrics().getNumNodeManagers();

   // ✅ GOOD - Wait for expected state
   cluster.start();
   GenericTestUtils.waitFor(() -> {
       try {
           return yarnClient.getYarnClusterMetrics().getNumNodeManagers() == 3;
       } catch (Exception e) {
           return false;
       }
   }, 1000, 30000);
   ```

3. **Keep Test Logs on Failure**:
   ```java
   @Rule
   public TestRule watcher = new TestWatcher() {
       protected void failed(Throwable e, Description description) {
           if (cluster != null) {
               DirectoryManager dirManager = cluster.getDirectoryManager();
               dirManager.setDeleteOnCleanup(false);
               System.err.println("Test failed. Logs at: " +
                   dirManager.getClusterRoot());
           }
       }
   };
   ```

---

## Debugging

### Enabling Debug Logging

```java
// In test setup
YarnConfiguration conf = new YarnConfiguration();

// Enable debug logging for YARN
conf.set("yarn.log.level", "DEBUG");

// Specific loggers
System.setProperty("org.apache.hadoop.yarn.level", "DEBUG");
System.setProperty("org.apache.hadoop.yarn.server.process.level", "DEBUG");
```

### Accessing Process Logs

```bash
# Find cluster root directory (printed at startup)
CLUSTER_ROOT=/tmp/process-miniyarn-1699564321123

# View RM logs
tail -f $CLUSTER_ROOT/rm0/logs/stderr.log

# View NM logs
tail -f $CLUSTER_ROOT/nm0/logs/stderr.log

# Search for errors
grep -r "ERROR" $CLUSTER_ROOT/*/logs/
```

### Debugging Subprocess Startup

```java
// Add to ProcessNodeManager
protected void start() throws IOException {
    List<String> command = buildCommand();

    // Print command for debugging
    System.err.println("Starting process with command:");
    System.err.println(String.join(" ", command));

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(false);
    process = pb.start();

    // Print environment
    System.err.println("Environment:");
    pb.environment().forEach((k, v) ->
        System.err.println("  " + k + "=" + v));
}
```

### Debugging Port Allocation

```java
PortAllocator allocator = new PortAllocator();

// Enable verbose logging
allocator.setVerbose(true);

int port = allocator.allocatePort();
System.out.println("Allocated port: " + port);

// Verify port is actually in use
try (ServerSocket ss = new ServerSocket(port)) {
    // Should fail if port actually allocated
    System.err.println("ERROR: Port not actually in use!");
} catch (IOException e) {
    System.out.println("Port correctly allocated");
}
```

### Debugging Version Incompatibility

```java
String version1 = "3.3.6";
String version2 = "3.4.0";

// Check compatibility
boolean compatible = VersionConfigAdapter.areCompatible(version1, version2);
System.out.println("Compatible: " + compatible);

// Get upgrade path recommendation
String path = UpgradeTestHelper.getUpgradePath(version1, version2);
System.out.println("Upgrade path: " + path);

// Inspect adapted configuration
VersionConfigAdapter adapter = VersionConfigAdapter.forVersion(version2);
YarnConfiguration adapted = adapter.adapt(conf, version1);

// Print all config changes
Iterator<Map.Entry<String, String>> it = adapted.iterator();
while (it.hasNext()) {
    Map.Entry<String, String> entry = it.next();
    String original = conf.get(entry.getKey());
    if (!entry.getValue().equals(original)) {
        System.out.println("Changed: " + entry.getKey() +
            " = " + original + " → " + entry.getValue());
    }
}
```

---

## Performance Considerations

### Startup Performance

**Bottlenecks**:
1. Subprocess JVM startup (200-500ms per process)
2. YARN node initialization (1-3 seconds per node)
3. Node registration with RM (heartbeat-based)

**Optimizations**:
```java
YarnConfiguration conf = new YarnConfiguration();

// Reduce heartbeat intervals
conf.setInt(YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 500);  // Default: 1000

// Reduce expiry interval
conf.setLong(YarnConfiguration.RM_NM_EXPIRY_INTERVAL_MS, 10000);  // Default: 600000

// Faster state store writes
conf.setInt("yarn.resourcemanager.state-store.num-retries", 0);

// Minimize logging
conf.set("yarn.log.level", "WARN");
```

### Memory Usage

**Per-Process Overhead**:
- JVM overhead: ~50-100 MB
- YARN RM: ~256 MB minimum
- YARN NM: ~128 MB minimum

**Minimize Memory**:
```java
// In ProcessNodeManager.buildCommand()
command.add("-Xms128m");
command.add("-Xmx512m");  // Adjust based on test needs
```

### Parallelizing Tests

```java
// Use JUnit parallel execution
@RunWith(Parameterized.class)
public class ParallelUpgradeTests {
    @Parameterized.Parameters(name = "{index}: upgrade from {0} to {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"3.3.5", "3.3.6"},
            {"3.3.6", "3.4.0"},
            {"3.4.0", "3.4.1"}
        });
    }

    // Each parameter set runs in parallel
}
```

---

## Contribution Guidelines

### Code Style

- Follow Apache Hadoop coding standards
- Use SLF4J for logging (`LOG.info`, `LOG.debug`, `LOG.error`)
- Comprehensive Javadoc on public classes and methods
- Unit tests for all new components
- Integration tests for new features

### Submitting Changes

1. **Create JIRA ticket**:
   ```
   YARN-XXXXX: Add support for XYZ in ProcessBasedMiniYARNCluster
   ```

2. **Create patch**:
   ```bash
   git diff > YARN-XXXXX.patch
   ```

3. **Run tests**:
   ```bash
   mvn test -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
   ```

4. **Check style**:
   ```bash
   mvn checkstyle:checkstyle -pl hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests
   ```

5. **Upload patch to JIRA**

### Review Checklist

- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] New tests added for new functionality
- [ ] Javadoc complete and accurate
- [ ] No checkstyle violations
- [ ] Backward compatibility maintained
- [ ] Documentation updated (if user-facing change)

---

## See Also

- **User Guide**: `ProcessBasedMiniYARNCluster-UserGuide.md` - Usage examples and API reference
- **Version Compatibility**: `VERSION_COMPATIBILITY.md` - Multi-version testing guide
- **Apache Hadoop**: https://hadoop.apache.org/ - Official documentation

---

**Document Version**: 1.0
**Last Updated**: 2025-11-09
**Contributions**: Welcome! See contribution guidelines above.
