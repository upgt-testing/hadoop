# ProcessBasedMiniDFSCluster User Guide

## Overview

`ProcessBasedMiniDFSCluster` is a testing framework for Apache Hadoop HDFS that runs NameNodes and DataNodes in separate JVM processes. This enables testing scenarios that are impossible with the traditional in-process `MiniDFSCluster`, including:

- **Mixed-version clusters**: Run different Hadoop versions on different nodes
- **Version upgrades**: Test rolling upgrade scenarios
- **Process isolation**: True multi-process behavior for realistic testing
- **Compatibility testing**: Verify cross-version compatibility

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Basic Usage](#basic-usage)
4. [Advanced Scenarios](#advanced-scenarios)
5. [Configuration Options](#configuration-options)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

## Prerequisites

### Required Software

1. **JDK 8 or higher**
   ```bash
   java -version
   # Should output: java version "1.8.0" or higher
   ```

2. **Maven 3.3+** (for building)
   ```bash
   mvn -version
   ```

3. **Hadoop Distributions**
   - At least one built Hadoop distribution
   - For mixed-version testing: multiple Hadoop distributions

### Setting Up Hadoop Distributions

You need complete Hadoop distributions (with all JARs and dependencies) for each version you want to test:

```bash
# Download and extract Hadoop distributions
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.1/hadoop-3.3.1.tar.gz
tar xzf hadoop-3.3.1.tar.gz -C /opt/

wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.5/hadoop-3.3.5.tar.gz
tar xzf hadoop-3.3.5.tar.gz -C /opt/
```

**System Properties (Recommended)**:
Pass Hadoop distribution paths via Maven system properties:

```bash
# For tests
mvn test -Dtest=MyTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6
```

**Environment Variables (Fallback)**:
Alternatively, use environment variables (still supported):

```bash
export HADOOP_HOME=/opt/hadoop-3.3.5
export HADOOP_UPGRADE_HOME=/opt/hadoop-3.3.6
```

### System Requirements

- **Memory**: At least 4GB RAM (2GB for cluster + 2GB for tests)
- **Disk**: 1GB free space for temporary files
- **Ports**: Range 50000-59999 should be available (configurable)

## Quick Start

### 5-Minute Example

Here's a complete example to get you started:

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;

public class QuickStartExample {
    public static void main(String[] args) throws Exception {
        // 1. Create configuration
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "3");

        // 2. Build cluster (automatically reads hadoop.start.home from system properties)
        ProcessBasedMiniDFSCluster cluster =
            new ProcessBasedMiniDFSCluster.Builder(conf)
                .numDataNodes(3)
                .format(true)
                .build();

        // 3. Wait for cluster to be ready
        cluster.waitClusterUp();

        // 4. Use the cluster
        FileSystem fs = cluster.getFileSystem();
        Path testFile = new Path("/test.txt");

        // Write data
        fs.createNewFile(testFile);
        System.out.println("Created file: " + testFile);

        // 5. Cleanup
        cluster.shutdown();
        System.out.println("Cluster shut down successfully");
    }
}
```

Run this example:
```bash
# Pass Hadoop distribution via system properties
mvn compile exec:java -Dexec.mainClass="QuickStartExample" \
  -Dhadoop.start.home=/opt/hadoop-3.3.5
```

## Basic Usage

### Creating a Simple Cluster

The most basic cluster setup with all nodes running the same Hadoop version:

```java
Configuration conf = new HdfsConfiguration();
conf.set("dfs.replication", "2");

// Automatically reads hadoop.start.home from system properties
ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

cluster.waitClusterUp();
```

**Note**: Pass the Hadoop distribution via Maven system properties:
```bash
mvn test -Dtest=MyTest -Dhadoop.start.home=/opt/hadoop-3.3.5
```

### Cluster Lifecycle

#### Starting the Cluster

The cluster starts automatically when you call `build()`. To wait for full readiness:

```java
cluster.waitClusterUp();  // Blocks until cluster is healthy
```

#### Accessing the FileSystem

```java
FileSystem fs = cluster.getFileSystem();

// Use standard Hadoop FileSystem API
Path file = new Path("/data/test.txt");
FSDataOutputStream out = fs.create(file);
out.writeUTF("Hello HDFS!");
out.close();

// Read data
FSDataInputStream in = fs.open(file);
String data = in.readUTF();
in.close();

System.out.println("Read: " + data);
```

#### Shutting Down

Always shut down the cluster to clean up resources:

```java
cluster.shutdown();  // Graceful shutdown, keeps data directories

// Or, delete all data:
cluster.shutdown(true);  // Delete temporary directories
```

### Using in JUnit Tests

Recommended pattern for JUnit tests:

```java
public class MyHDFSTest {
    private ProcessBasedMiniDFSCluster cluster;
    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        Configuration conf = new HdfsConfiguration();
        conf.set("dfs.replication", "2");

        // Automatically reads hadoop.start.home from system properties
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(3)
            .format(true)
            .build();

        cluster.waitClusterUp();
        fs = cluster.getFileSystem();
    }

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown(true);  // Clean up after each test
        }
    }

    @Test
    public void testFileOperations() throws Exception {
        Path testFile = new Path("/test.txt");
        assertTrue(fs.createNewFile(testFile));
        assertTrue(fs.exists(testFile));
    }
}
```

Run the test with:
```bash
mvn test -Dtest=MyHDFSTest -Dhadoop.start.home=/opt/hadoop-3.3.5
```

## Advanced Scenarios

### Mixed-Version Clusters

Run different Hadoop versions on different nodes:

```java
ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution("/opt/hadoop-3.3.1")
        .dataNodeHadoopDistribution(0, "/opt/hadoop-3.3.1")
        .dataNodeHadoopDistribution(1, "/opt/hadoop-3.3.5")
        .dataNodeHadoopDistribution(2, "/opt/hadoop-3.3.6")
        .format(true)
        .build();
```

**Use Cases:**
- Testing forward/backward compatibility
- Simulating upgrade scenarios
- Testing protocol compatibility

### Rolling Upgrades

Simulate a production rolling upgrade:

```java
import org.apache.hadoop.hdfs.server.process.upgrade.UpgradeTestHelper;

// Start with start version (automatically reads hadoop.start.home)
ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .format(true)
        .build();

cluster.waitClusterUp();
FileSystem fs = cluster.getFileSystem();

// Write test data
List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

// Perform rolling upgrade to upgrade version (automatically uses hadoop.upgrade.home)
String targetVersion = cluster.getUpgradeDistributionPath();
UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, targetVersion, testFiles);

// Verify data after upgrade
UpgradeTestHelper.verifyTestData(fs, testFiles);

System.out.println("Rolling upgrade successful!");
cluster.shutdown();
```

Run the test with:
```bash
mvn test -Dtest=MyUpgradeTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.1 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.5
```

### Node Restart Testing

Test cluster behavior when nodes restart:

```java
// Restart a specific DataNode
cluster.restartDataNode(0);
cluster.waitClusterUp();

// Restart with version change
cluster.shutdownDataNode(1);
cluster.changeDataNodeVersion(1, "/opt/hadoop-3.3.6");
cluster.startDataNode(1);
cluster.waitClusterUp();

// Restart NameNode
cluster.restartNameNode(0);
cluster.waitClusterUp();
```

### Dynamic DataNode Addition

Add DataNodes to a running cluster (new in this version):

```java
Configuration conf = new HdfsConfiguration();
conf.set("dfs.replication", "2");

// Start with 2 DataNodes (automatically reads hadoop.start.home)
ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(2)
        .format(true)
        .build();

cluster.waitClusterUp();
FileSystem fs = cluster.getFileSystem();

// Write initial data
Path testFile = new Path("/test-data.txt");
fs.createNewFile(testFile);

// Dynamically add 3 more DataNodes
cluster.startDataNodes(conf, 3, true, null, null, null);

// Cluster now has 5 DataNodes total
System.out.println("Total DataNodes: " + cluster.getNumDataNodes());  // Prints: 5

// Data remains accessible
assertTrue(fs.exists(testFile));
```

**With custom storage types:**

```java
// Add 2 DataNodes with heterogeneous storage
StorageType[][] newStorageTypes = new StorageType[2][];
newStorageTypes[0] = new StorageType[]{StorageType.SSD, StorageType.DISK};
newStorageTypes[1] = new StorageType[]{StorageType.DISK, StorageType.ARCHIVE};

cluster.startDataNodes(conf, 2, newStorageTypes, true, null, null, null, null);
```

**Use cases for dynamic DataNode addition:**
- Testing cluster expansion scenarios
- Simulating dynamic scaling
- Testing rebalancing after node addition
- Testing storage policy behavior with new nodes

### Custom Configuration Per Node

```java
Configuration baseConf = new HdfsConfiguration();
baseConf.set("dfs.replication", "3");

// Build cluster with custom configuration (automatically reads hadoop.start.home)
ProcessBasedMiniDFSCluster cluster =
    new ProcessBasedMiniDFSCluster.Builder(baseConf)
        .numDataNodes(3)
        .format(true)
        .build();
```

## Configuration Options

### Builder Methods

| Method | Description | Example |
|--------|-------------|---------|
| `numDataNodes(int)` | Number of DataNodes (initial) | `.numDataNodes(3)` |
| `format(boolean)` | Format HDFS on startup | `.format(true)` |
| `storageTypes(StorageType[][])` | Set storage types per DataNode | `.storageTypes(types)` |
| `storagesPerDatanode(int)` | Number of storage locations per DN | `.storagesPerDatanode(2)` |
| `nameNodeHadoopDistribution(String)` | Set NameNode version (for mixed-version) | `.nameNodeHadoopDistribution("/opt/hadoop-3.3.1")` |
| `dataNodeHadoopDistribution(int, String)` | Set specific DataNode version (for mixed-version) | `.dataNodeHadoopDistribution(0, "/opt/hadoop-3.3.5")` |

**Note**: For uniform-version clusters, distributions are **automatically read** from system properties (`hadoop.start.home` and `hadoop.upgrade.home`). Explicit distribution methods are only needed for mixed-version testing.

### Cluster Management Methods

| Method | Description | Example |
|--------|-------------|---------|
| `startDataNodes(conf, num, manageDfsDirs, racks, hosts, capacities)` | Add DataNodes dynamically | `cluster.startDataNodes(conf, 3, true, null, null, null)` |
| `startDataNodes(conf, num, storageTypes, ...)` | Add DataNodes with storage types | `cluster.startDataNodes(conf, 2, types, true, ...)` |
| `restartDataNode(int)` | Restart a specific DataNode | `cluster.restartDataNode(0)` |
| `shutdownDataNode(int)` | Shutdown a specific DataNode | `cluster.shutdownDataNode(1)` |
| `changeDataNodeVersion(int, String)` | Change DataNode version | `cluster.changeDataNodeVersion(0, hadoopHome)` |
| `getNumDataNodes()` | Get current DataNode count | `int count = cluster.getNumDataNodes()` |
| `triggerHeartbeats()` | Wait for DataNode heartbeats | `cluster.triggerHeartbeats()` |
| `triggerBlockReports()` | Wait for DataNode block reports | `cluster.triggerBlockReports()` |

### Configuration Properties

Common HDFS configuration properties:

```java
Configuration conf = new HdfsConfiguration();

// Replication factor
conf.set("dfs.replication", "3");

// Block size (128MB)
conf.set("dfs.blocksize", "134217728");

// DataNode handler threads
conf.set("dfs.datanode.handler.count", "10");

// Enable permissions
conf.set("dfs.permissions.enabled", "false");
```

## Troubleshooting

### Common Issues

#### 1. "Cannot find Hadoop distribution" or "Start version not found"

**Problem**: The Hadoop distribution system property is not set or path doesn't exist.

**Solution**:
```bash
# Verify the path exists
ls -la /opt/hadoop-3.3.5

# Pass system property via Maven
mvn test -Dtest=MyTest -Dhadoop.start.home=/opt/hadoop-3.3.5

# For upgrade tests, also set:
mvn test -Dtest=MyTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6

# Or use environment variables (fallback):
export HADOOP_HOME=/opt/hadoop-3.3.5
export HADOOP_UPGRADE_HOME=/opt/hadoop-3.3.6
```

#### 2. "Port already in use" or "Address already bound"

**Problem**: Required ports are already in use.

**Solution**:
```bash
# Check what's using ports in range 50000-59999
lsof -i :50000-59999

# Kill stuck processes
pkill -f "ProcessLauncher"

# Or change port range (in future versions)
```

#### 3. "Cluster did not come up" or Timeout

**Problem**: Nodes failed to start or become healthy.

**Solution**:
```bash
# Check logs in temporary directory
ls -la /tmp/process-minicluster-*/*/logs/

# View NameNode logs
tail -100 /tmp/process-minicluster-*/nn0/logs/*

# View DataNode logs
tail -100 /tmp/process-minicluster-*/dn0/logs/*

# Common causes:
# - Insufficient memory
# - Port conflicts
# - Incorrect Hadoop distribution
# - Java version mismatch
```

#### 4. "Version compatibility" errors

**Problem**: Incompatible Hadoop versions used together.

**Solution**:
```java
// Check compatibility before building cluster
VersionConfigAdapter v1 = new VersionConfigAdapter("3.3.1");
VersionConfigAdapter v2 = new VersionConfigAdapter("3.3.5");

if (v1.isCompatibleWith(v2)) {
    System.out.println("Versions are compatible");
} else {
    System.out.println("Versions are NOT compatible");
}

// Generally compatible:
// - Same major version (e.g., 3.x with 3.x)
// - Adjacent minor versions in 2.x (e.g., 2.9 with 2.10)

// NOT compatible:
// - Different major versions (e.g., 2.x with 3.x)
```

#### 5. "Out of Memory" errors

**Problem**: Not enough heap for multiple JVM processes.

**Solution**:
```bash
# Increase test JVM memory
export MAVEN_OPTS="-Xmx4g"

# Or in surefire configuration
mvn test -DargLine="-Xmx4g"

# Reduce number of DataNodes for testing
.numDataNodes(2)  // Instead of 3 or more
```

### Debug Mode

Enable debug logging:

```java
// In your test
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
root.setLevel(Level.DEBUG);
```

Or via log4j.properties:
```properties
log4j.logger.org.apache.hadoop.hdfs.server.process=DEBUG
```

## Best Practices

### 1. Always Clean Up

```java
try {
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .build();  // Automatic - reads system properties!

    // Your test code

} finally {
    if (cluster != null) {
        cluster.shutdown(true);  // Always cleanup
    }
}
```

### 2. Use System Properties for Distribution Paths

```bash
# Pass distributions via Maven system properties (recommended)
mvn test -Dtest=MyTest \
  -Dhadoop.start.home=/opt/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/hadoop-3.3.6

# Or use environment variables (fallback)
export HADOOP_HOME=/opt/hadoop-3.3.5
export HADOOP_UPGRADE_HOME=/opt/hadoop-3.3.6
```

No code changes needed - distributions are automatically read!

### 3. Test Data Verification

```java
import org.apache.hadoop.hdfs.server.process.upgrade.UpgradeTestHelper;

// Write test data
List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

// ... perform operations ...

// Verify data integrity
UpgradeTestHelper.verifyTestData(fs, testFiles);

// Cleanup
UpgradeTestHelper.cleanupTestData(fs, testFiles);
```

### 4. Wait for Cluster Stability

```java
// After any node operation
cluster.restartDataNode(0);
cluster.waitClusterUp();  // Wait before proceeding

// For custom checks
UpgradeTestHelper.waitForClusterStable(cluster, 30000);  // 30 second timeout
```

### 5. Use Appropriate Replication

```java
// For 3 DataNodes, use replication <= 3
conf.set("dfs.replication", "2");  // or "3"

// Don't use:
conf.set("dfs.replication", "5");  // Only 3 DNs available!
```

## Example: Complete Integration Test

```java
@Test
public void testCompleteWorkflow() throws Exception {
    Configuration conf = new HdfsConfiguration();
    conf.set("dfs.replication", "3");

    ProcessBasedMiniDFSCluster cluster = null;
    try {
        // 1. Build cluster (automatically reads hadoop.start.home)
        cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
            .numDataNodes(3)
            .format(true)
            .build();

        cluster.waitClusterUp();
        FileSystem fs = cluster.getFileSystem();

        // 2. Write data
        Path testDir = new Path("/test");
        fs.mkdirs(testDir);

        for (int i = 0; i < 10; i++) {
            Path file = new Path(testDir, "file-" + i + ".txt");
            FSDataOutputStream out = fs.create(file);
            out.writeUTF("Test data " + i);
            out.close();
        }

        // 3. Verify data
        FileStatus[] files = fs.listStatus(testDir);
        assertEquals(10, files.length);

        // 4. Test node restart
        cluster.restartDataNode(0);
        cluster.waitClusterUp();

        // 5. Verify data still accessible
        files = fs.listStatus(testDir);
        assertEquals(10, files.length);

        // 6. Read specific file
        Path file0 = new Path(testDir, "file-0.txt");
        FSDataInputStream in = fs.open(file0);
        String data = in.readUTF();
        in.close();
        assertEquals("Test data 0", data);

        // 7. Cleanup
        fs.delete(testDir, true);

    } finally {
        if (cluster != null) {
            cluster.shutdown(true);
        }
    }
}
```

Run the test with:
```bash
mvn test -Dtest=MyTest -Dhadoop.start.home=/opt/hadoop-3.3.5
```

## Additional Resources

- [Developer Guide](ProcessBasedMiniDFSCluster-DeveloperGuide.md) - For extending the framework
- [Version Upgrade Testing Guide](VersionUpgradeTestingGuide.md) - Detailed upgrade testing scenarios
- [CLAUDE.md](../../CLAUDE.md) - Project-specific development guidelines

## Support

For issues and questions:
- Check the troubleshooting section above
- Review test examples in `src/test/java/org/apache/hadoop/hdfs/server/process/`
- File issues in the project's issue tracker
