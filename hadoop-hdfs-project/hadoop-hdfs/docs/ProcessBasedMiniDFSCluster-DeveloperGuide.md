# ProcessBasedMiniDFSCluster Developer Guide

## Overview

This guide is for developers who want to understand, extend, or contribute to the ProcessBasedMiniDFSCluster framework. It covers the architecture, implementation details, and guidelines for adding new features.

## Table of Contents

1. [Architecture](#architecture)
2. [Core Components](#core-components)
3. [Process Lifecycle](#process-lifecycle)
4. [Adding New Features](#adding-new-features)
5. [Testing Guidelines](#testing-guidelines)
6. [Debugging Tips](#debugging-tips)
7. [Contributing](#contributing)

## Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────┐
│              Test JVM Process                           │
│  ┌───────────────────────────────────────────────────┐ │
│  │     ProcessBasedMiniDFSCluster                    │ │
│  │  ┌────────────────┐  ┌────────────────┐          │ │
│  │  │ NN Process Mgr │  │ DN Process Mgr │          │ │
│  │  └────────┬───────┘  └────────┬───────┘          │ │
│  │           │ RPC                 │ RPC             │ │
│  └───────────┼─────────────────────┼─────────────────┘ │
└──────────────┼─────────────────────┼───────────────────┘
               │                     │
       ┌───────▼────────┐    ┌──────▼─────────┐
       │  NN Process    │    │  DN Process    │
       │  Hadoop v3.3.1 │    │  Hadoop v3.3.5 │
       └────────────────┘    └────────────────┘
```

### Design Principles

1. **Process Isolation**: Each node runs in its own JVM with isolated classpath
2. **RPC-Only Communication**: Cluster management uses only RPC/socket communication
3. **Version Independence**: Support for mixed Hadoop versions via classpath isolation
4. **Builder Pattern**: Flexible cluster configuration through fluent API
5. **Extensibility**: Modular design for adding new node types or features

### Module Structure

```
org.apache.hadoop.hdfs.server.process/
├── ProcessBasedMiniDFSCluster.java      # Main cluster coordinator
├── ProcessNodeManager.java               # Base process manager
├── NameNodeProcessManager.java           # NameNode-specific manager
├── DataNodeProcessManager.java           # DataNode-specific manager
├── HadoopDistribution.java              # Distribution discovery
├── HadoopVersionRegistry.java           # Version management
├── ClasspathBuilder.java                # Classpath construction
├── PortAllocator.java                   # Port allocation
├── DirectoryManager.java                # Directory structure
├── ProcessConfigurationGenerator.java   # Config generation
├── VersionConfigAdapter.java            # Version compatibility
└── launcher/
    ├── NameNodeProcessLauncher.java     # NN subprocess entry point
    └── DataNodeProcessLauncher.java     # DN subprocess entry point
```

## Core Components

### 1. ProcessBasedMiniDFSCluster

**Purpose**: Main coordinator class that manages the entire cluster.

**Key Responsibilities**:
- Build and configure cluster via Builder pattern
- Manage process lifecycles
- Provide client-side API (getFileSystem, restart methods)
- Coordinate cluster startup and shutdown

**Important Methods**:
```java
// Building
public static class Builder {
    public Builder numDataNodes(int num)
    public Builder fromSystemProperties()  // Reads hadoop.start.home and hadoop.upgrade.home
    public Builder nameNodeHadoopDistribution(String path)  // For mixed-version only
    public Builder dataNodeHadoopDistribution(int index, String path)  // For mixed-version only
    public ProcessBasedMiniDFSCluster build()  // Automatically calls fromSystemProperties() if needed
}

// Lifecycle
public void waitClusterUp()
public void shutdown()
public void shutdown(boolean deleteDirs)

// Node operations
public void restartNameNode(int nnIndex)
public void restartDataNode(int dnIndex)
public void shutdownDataNode(int dnIndex)
public void startDataNode(int dnIndex)
public void changeDataNodeVersion(int dnIndex, String hadoopHome)

// Client API
public FileSystem getFileSystem()
public URI getURI()
public int getNumDataNodes()
public String getUpgradeDistributionPath()  // Get upgrade version path
```

**Design Pattern**: Builder + Facade
```java
// Builder handles configuration - automatically reads system properties!
Builder builder = new Builder(conf)
    .numDataNodes(3);
    // No need to set distribution - automatic!

// Facade provides simple interface
ProcessBasedMiniDFSCluster cluster = builder.build();
FileSystem fs = cluster.getFileSystem();

// For upgrade tests, access upgrade version:
String upgradeHome = cluster.getUpgradeDistributionPath();
```

**Note**: Distributions are **automatically read** from system properties (`hadoop.start.home` and `hadoop.upgrade.home`) when calling `.build()`. Explicit distribution methods are only needed for mixed-version testing.

### 2. ProcessNodeManager (Base Class)

**Purpose**: Abstract base class for managing individual node processes.

**Key Responsibilities**:
- Start/stop processes
- Monitor process health
- Build classpaths
- Manage working directories

**Key Fields**:
```java
protected Process process;              // The running process
protected Configuration nodeConfig;      // Node configuration
protected String hadoopHome;            // Hadoop distribution path
protected File workDir;                 // Node working directory
protected int nodeIndex;                // Node index in cluster
```

**Extension Points**:
```java
// Subclasses must implement:
abstract void start() throws IOException, TimeoutException;
abstract void stop() throws IOException;
abstract boolean isHealthy() throws IOException;
abstract InetSocketAddress getRpcAddress();
```

### 3. NameNodeProcessManager

**Purpose**: Manages NameNode processes.

**Specific Implementation**:
```java
@Override
public void start() throws IOException, TimeoutException {
    // 1. Build classpath for this Hadoop version
    List<String> classpath = buildClasspath();

    // 2. Build command
    List<String> command = Arrays.asList(
        "java",
        "-cp", String.join(":", classpath),
        "org.apache.hadoop.hdfs.server.process.launcher.NameNodeProcessLauncher",
        "--config-dir", workDir.getAbsolutePath() + "/conf",
        "--node-index", String.valueOf(nodeIndex)
    );

    // 3. Start process
    ProcessBuilder pb = new ProcessBuilder(command);
    process = pb.start();

    // 4. Wait for ready
    waitForProcessReady(30000);
}

@Override
public boolean isHealthy() throws IOException {
    // Check via ClientProtocol RPC
    ClientProtocol nn = getNameNodeRpcProxy();
    try {
        nn.getFileInfo("/");  // Simple RPC call
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

### 4. DataNodeProcessManager

**Purpose**: Manages DataNode processes.

**Key Differences from NameNode**:
- Health check via DataNode registration with NameNode
- Different ports (data transfer, IPC, HTTP)
- Requires NameNode address for registration

### 5. HadoopDistribution

**Purpose**: Discovers and validates Hadoop distributions.

**Key Methods**:
```java
public HadoopDistribution(File hadoopHome) {
    // Discover JARs
    this.coreJars = findJars(hadoopHome, "share/hadoop/common");
    this.hdfsJars = findJars(hadoopHome, "share/hadoop/hdfs");
    this.libJars = findJars(hadoopHome, "share/hadoop/*/lib");

    // Validate
    validateDistribution();
}

public List<File> getAllJars() {
    List<File> all = new ArrayList<>();
    all.addAll(coreJars);
    all.addAll(hdfsJars);
    all.addAll(libJars);
    return all;
}
```

### 6. VersionConfigAdapter

**Purpose**: Handles configuration differences between Hadoop versions.

**Key Features**:
- Parse version strings (e.g., "3.3.5", "2.10.2")
- Map deprecated configuration keys
- Check feature availability by version
- Validate version compatibility

**Example Usage**:
```java
VersionConfigAdapter adapter = new VersionConfigAdapter("3.3.5");

// Check compatibility
if (adapter.isCompatibleWith(otherAdapter)) {
    // Versions can work together
}

// Adapt configuration
Configuration adapted = adapter.adaptConfiguration(baseConfig);

// Check feature availability
if (adapter.isFeatureAvailable("ERASURE_CODING")) {
    // Enable erasure coding
}
```

## Process Lifecycle

### Startup Sequence

```
1. ProcessBasedMiniDFSCluster.build()
   ├─ Validate builder configuration
   ├─ Create HadoopDistribution objects
   ├─ Allocate ports via PortAllocator
   ├─ Create work directories via DirectoryManager
   ├─ Generate configurations via ProcessConfigurationGenerator
   │
   ├─ 2. Start NameNodes
   │  ├─ For each NameNode:
   │  │  ├─ Create NameNodeProcessManager
   │  │  ├─ Build classpath (version-specific)
   │  │  ├─ Write configuration files
   │  │  ├─ Start JVM process
   │  │  ├─ Wait for RPC server ready
   │  │  └─ Verify health
   │  │
   │  └─ Wait for all NameNodes healthy
   │
   ├─ 3. Start DataNodes
   │  ├─ For each DataNode:
   │  │  ├─ Create DataNodeProcessManager
   │  │  ├─ Build classpath (version-specific)
   │  │  ├─ Write configuration files
   │  │  ├─ Start JVM process
   │  │  ├─ Wait for registration with NameNode
   │  │  └─ Verify health
   │  │
   │  └─ Wait for all DataNodes registered
   │
   └─ 4. Final verification
      ├─ All nodes healthy
      ├─ Cluster out of safe mode
      └─ Basic operations work
```

### Shutdown Sequence

```
1. ProcessBasedMiniDFSCluster.shutdown()
   ├─ Close FileSystem instances
   │
   ├─ 2. Stop DataNodes
   │  └─ For each DataNode:
   │     ├─ Try graceful shutdown (via RPC if available)
   │     ├─ Wait for process exit (5s timeout)
   │     ├─ Send SIGTERM if needed
   │     ├─ Send SIGKILL if still running
   │     └─ Release resources
   │
   ├─ 3. Stop NameNodes
   │  └─ For each NameNode:
   │     └─ Same shutdown procedure as DataNodes
   │
   └─ 4. Cleanup (if deleteDirs=true)
      ├─ Delete work directories
      ├─ Release ports
      └─ Clear registry
```

## Adding New Features

### Example: Adding JournalNode Support

1. **Create JournalNodeProcessManager**:

```java
public class JournalNodeProcessManager extends ProcessNodeManager {
    private InetSocketAddress rpcAddress;
    private InetSocketAddress httpAddress;

    public JournalNodeProcessManager(Configuration nodeConfig,
                                      String hadoopHome,
                                      File workDir,
                                      int nodeIndex) {
        super(nodeConfig, hadoopHome, workDir, nodeIndex);
        extractAddresses(nodeConfig);
    }

    @Override
    public void start() throws IOException, TimeoutException {
        // Build classpath
        List<String> classpath = buildClasspath();

        // Build command
        List<String> command = Arrays.asList(
            "java",
            "-cp", String.join(":", classpath),
            "org.apache.hadoop.hdfs.server.process.launcher.JournalNodeProcessLauncher",
            "--config-dir", workDir.getAbsolutePath() + "/conf",
            "--node-index", String.valueOf(nodeIndex)
        );

        // Start process
        ProcessBuilder pb = new ProcessBuilder(command);
        process = pb.start();

        // Wait for ready
        waitForProcessReady(30000);
    }

    @Override
    public boolean isHealthy() throws IOException {
        // Check if JournalNode RPC is responsive
        return isPortOpen(rpcAddress);
    }

    @Override
    public InetSocketAddress getRpcAddress() {
        return rpcAddress;
    }

    @Override
    protected List<String> buildClasspath() {
        // Same as other nodes - reuse parent implementation
        return super.buildClasspath();
    }
}
```

2. **Create JournalNodeProcessLauncher**:

```java
public class JournalNodeProcessLauncher {
    public static void main(String[] args) throws Exception {
        // Parse arguments
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("config-dir", true, "Configuration directory");
        options.addOption("node-index", true, "Node index");
        CommandLine cmd = parser.parse(options, args);

        String configDir = cmd.getOptionValue("config-dir");
        int nodeIndex = Integer.parseInt(cmd.getOptionValue("node-index"));

        // Load configuration
        Configuration conf = new Configuration();
        conf.addResource(new Path(configDir, "core-site.xml"));
        conf.addResource(new Path(configDir, "hdfs-site.xml"));

        // Start JournalNode
        JournalNode jn = new JournalNode(conf);
        jn.start();

        // Wait
        jn.join();
    }
}
```

3. **Update ProcessBasedMiniDFSCluster**:

```java
public class ProcessBasedMiniDFSCluster {
    private List<JournalNodeProcessManager> journalNodeManagers = new ArrayList<>();

    public static class Builder {
        private int numJournalNodes = 0;

        public Builder numJournalNodes(int num) {
            this.numJournalNodes = num;
            return this;
        }
    }

    private void startJournalNodes() throws Exception {
        for (int i = 0; i < numJournalNodes; i++) {
            // Generate configuration
            Configuration jnConfig = configGenerator.generateJournalNodeConfig(
                baseConfiguration, i, journalNodeWorkDir);

            // Create manager
            JournalNodeProcessManager jnMgr = new JournalNodeProcessManager(
                jnConfig, hadoopHome, journalNodeWorkDir, i);

            // Start
            jnMgr.start();
            journalNodeManagers.add(jnMgr);
        }
    }
}
```

### Example: Adding Prometheus Metrics

1. **Create MetricsCollector**:

```java
public class ClusterMetricsCollector {
    private ProcessBasedMiniDFSCluster cluster;

    public ClusterMetrics collect() {
        ClusterMetrics metrics = new ClusterMetrics();
        metrics.numNameNodes = cluster.getNumNameNodes();
        metrics.numDataNodes = cluster.getNumDataNodes();

        // Collect from each node
        for (int i = 0; i < cluster.getNumDataNodes(); i++) {
            DataNodeMetrics dnMetrics = collectDataNodeMetrics(i);
            metrics.addDataNodeMetrics(dnMetrics);
        }

        return metrics;
    }

    private DataNodeMetrics collectDataNodeMetrics(int index) {
        // Query via JMX or HTTP endpoint
        // ...
    }
}
```

2. **Integrate into cluster**:

```java
public class ProcessBasedMiniDFSCluster {
    private ClusterMetricsCollector metricsCollector;

    public ClusterMetrics getMetrics() {
        if (metricsCollector == null) {
            metricsCollector = new ClusterMetricsCollector(this);
        }
        return metricsCollector.collect();
    }
}
```

## Testing Guidelines

### Unit Test Structure

```java
@Test
public void testFeatureName() throws Exception {
    // 1. Setup
    Configuration conf = new HdfsConfiguration();
    conf.set("property", "value");

    // 2. Execute
    ResultType result = methodUnderTest(conf);

    // 3. Verify
    assertEquals(expected, result);
    assertTrue(condition);

    // 4. Cleanup (if needed)
    cleanup();
}
```

### Integration Test Structure

```java
@Test
public void testIntegrationScenario() throws Exception {
    ProcessBasedMiniDFSCluster cluster = null;
    try {
        // 1. Build cluster (automatically reads hadoop.start.home from system properties)
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(3)
            .build();

        cluster.waitClusterUp();

        // 2. Execute test scenario
        FileSystem fs = cluster.getFileSystem();
        // ... test operations ...

        // 3. Verify results
        assertTrue(fs.exists(new Path("/test")));

    } finally {
        // 4. Always cleanup
        if (cluster != null) {
            cluster.shutdown(true);
        }
    }
}
```

Run the test with:
```bash
mvn test -Dtest=MyIntegrationTest -Dhadoop.start.home=/opt/hadoop-3.3.5
```

### Test Naming Conventions

- **Unit tests**: `Test<ClassName>.java` (e.g., `TestPortAllocator.java`)
- **Integration tests**: `TestProcessBased<Feature>.java` (e.g., `TestProcessBasedClusterUpgrade.java`)
- **Test methods**: `test<Scenario>` (e.g., `testBasicFileOperations`)

### Test Categories

Use JUnit categories to organize tests:

```java
public interface UnitTest {}
public interface IntegrationTest {}
public interface SlowTest {}

@Category({IntegrationTest.class, SlowTest.class})
@Test
public void testRollingUpgrade() {
    // ...
}
```

Run specific categories:
```bash
mvn test -Dgroups="org.apache.hadoop.hdfs.UnitTest"
```

## Debugging Tips

### 1. Enable Debug Logging

```java
// In test setup
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@Before
public void setUp() {
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.DEBUG);

    // Or specific package
    Logger processLogger = (Logger) LoggerFactory.getLogger(
        "org.apache.hadoop.hdfs.server.process");
    processLogger.setLevel(Level.DEBUG);
}
```

### 2. Inspect Process Logs

```bash
# Find the temp directory
ls -ltr /tmp/ | grep process-minicluster

# View NameNode logs
tail -100 /tmp/process-minicluster-*/nn0/logs/*.log

# View DataNode logs
tail -100 /tmp/process-minicluster-*/dn*/logs/*.log

# Follow logs in real-time
tail -f /tmp/process-minicluster-*/nn0/logs/*.log
```

### 3. Debug Subprocess

To attach a debugger to a subprocess:

1. Modify process startup to add debug options:

```java
List<String> command = Arrays.asList(
    "java",
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005",
    "-cp", classpath,
    "org.apache.hadoop.hdfs.server.process.launcher.NameNodeProcessLauncher",
    // ... args ...
);
```

2. Attach debugger to port 5005

### 4. Check Process Status

```bash
# List all process launcher processes
ps aux | grep ProcessLauncher

# Check specific PID
ps -p <PID> -f

# Check process tree
pstree -p <PID>
```

### 5. Port Debugging

```bash
# Check what's using a port
lsof -i :50100

# Check all ports in range
for port in {50000..50010}; do
    lsof -i :$port 2>/dev/null && echo "Port $port in use"
done

# Kill process using port
lsof -t -i :50100 | xargs kill
```

### 6. Memory Debugging

```bash
# Monitor memory usage
watch -n 1 'ps aux | grep ProcessLauncher | awk "{print \$2, \$4, \$11}"'

# Get heap dump
jmap -dump:format=b,file=heap.hprof <PID>

# Analyze with jhat or VisualVM
jhat heap.hprof
```

## Contributing

### Code Style

Follow Apache Hadoop code style:
- Indentation: 2 spaces
- Line length: 100 characters
- Use JavaDoc for all public methods
- Add license header to all files

### Pull Request Process

1. Create feature branch: `git checkout -b feature/my-feature`
2. Implement feature with tests
3. Run all tests: `mvn clean test`
4. Run checkstyle: `mvn checkstyle:check`
5. Commit with descriptive message
6. Push and create PR

### Commit Message Format

```
HDFS-XXXXX. Brief description of change

Detailed explanation of what changed and why.

Test Plan:
- Describe how you tested the change
- List any new tests added
```

### Testing Requirements

- All new code must have unit tests (>80% coverage)
- Integration tests for user-facing features
- All existing tests must pass
- No new checkstyle violations

## Additional Resources

- [User Guide](ProcessBasedMiniDFSCluster-UserGuide.md)
- [Version Upgrade Testing Guide](VersionUpgradeTestingGuide.md)
- [Apache Hadoop Development Guidelines](https://hadoop.apache.org/docs/current/)
- [HDFS Architecture](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html)
