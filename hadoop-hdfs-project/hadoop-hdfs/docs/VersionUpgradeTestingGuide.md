# Version Upgrade Testing Guide

## Overview

This guide provides detailed instructions for testing Hadoop version upgrades using ProcessBasedMiniDFSCluster. It covers testing strategies, common scenarios, and best practices for ensuring smooth version transitions.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Testing Strategies](#testing-strategies)
3. [Common Upgrade Scenarios](#common-upgrade-scenarios)
4. [Version Compatibility Matrix](#version-compatibility-matrix)
5. [Test Data Management](#test-data-management)
6. [Troubleshooting Upgrades](#troubleshooting-upgrades)
7. [Best Practices](#best-practices)

## Prerequisites

### Required Hadoop Distributions

For comprehensive upgrade testing, you'll need multiple Hadoop distributions:

```bash
# Download and install Hadoop distributions
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.1/hadoop-3.3.1.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.5/hadoop-3.3.5.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz

# Extract to standard locations
tar xzf hadoop-3.3.1.tar.gz -C /opt/
tar xzf hadoop-3.3.5.tar.gz -C /opt/
tar xzf hadoop-3.3.6.tar.gz -C /opt/

# Set environment variables
export HADOOP_3_3_1_HOME=/opt/hadoop-3.3.1
export HADOOP_3_3_5_HOME=/opt/hadoop-3.3.5
export HADOOP_3_3_6_HOME=/opt/hadoop-3.3.6
```

### Test Environment Setup

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.server.process.ProcessBasedMiniDFSCluster;
import org.apache.hadoop.hdfs.server.process.upgrade.UpgradeTestHelper;

public class UpgradeTestBase {
    protected Configuration conf;
    protected ProcessBasedMiniDFSCluster cluster;

    @Before
    public void setUp() {
        conf = new HdfsConfiguration();
        conf.set("dfs.replication", "3");

        // Verify environment variables
        assertNotNull("HADOOP_3_3_1_HOME must be set",
            System.getenv("HADOOP_3_3_1_HOME"));
        assertNotNull("HADOOP_3_3_5_HOME must be set",
            System.getenv("HADOOP_3_3_5_HOME"));
    }

    @After
    public void tearDown() {
        if (cluster != null) {
            cluster.shutdown(true);
        }
    }
}
```

## Testing Strategies

### 1. Rolling Upgrade Testing

**Purpose**: Verify that nodes can be upgraded one-by-one without cluster downtime.

**Strategy**:
- Start cluster with old version
- Write test data
- Upgrade DataNodes one-by-one
- Verify data integrity after each upgrade
- Verify cluster remains operational throughout

**Example**:
```java
@Test
public void testRollingUpgrade() throws Exception {
    String sourceVersion = System.getenv("HADOOP_3_3_1_HOME");
    String targetVersion = System.getenv("HADOOP_3_3_5_HOME");

    // 1. Start with old version
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(sourceVersion)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    // 2. Write test data
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);
    LOG.info("Created {} test files", testFiles.size());

    // 3. Perform rolling upgrade
    for (int i = 0; i < 3; i++) {
        LOG.info("Upgrading DataNode {} from {} to {}", i, sourceVersion, targetVersion);

        // Shutdown DataNode
        cluster.shutdownDataNode(i);

        // Change version
        cluster.changeDataNodeVersion(i, targetVersion);

        // Restart DataNode
        cluster.startDataNode(i);

        // Wait for cluster stability
        cluster.waitClusterUp();

        // Verify data after each upgrade
        UpgradeTestHelper.verifyTestData(fs, testFiles);
        LOG.info("DataNode {} upgraded successfully", i);
    }

    // 4. Verify final state
    UpgradeTestHelper.assertCanReadWriteData(fs);
    LOG.info("Rolling upgrade completed successfully");

    // Cleanup
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

### 2. Compatibility Testing

**Purpose**: Verify that different versions can coexist and communicate.

**Strategy**:
- Start cluster with mixed versions
- Verify all nodes can register and communicate
- Test data operations across version boundaries
- Monitor for protocol errors or compatibility issues

**Example**:
```java
@Test
public void testMixedVersionCompatibility() throws Exception {
    String version331 = System.getenv("HADOOP_3_3_1_HOME");
    String version335 = System.getenv("HADOOP_3_3_5_HOME");
    String version336 = System.getenv("HADOOP_3_3_6_HOME");

    // Build mixed-version cluster
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .nameNodeHadoopDistribution(version335)
        .dataNodeHadoopDistribution(0, version331)
        .dataNodeHadoopDistribution(1, version335)
        .dataNodeHadoopDistribution(2, version336)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    // Verify all nodes registered
    assertEquals(3, cluster.getNumDataNodes());

    // Test data operations
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 20, 2 * 1024 * 1024);
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    // Test node restart with mixed versions
    cluster.restartDataNode(0);
    cluster.waitClusterUp();
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

### 3. Downgrade Testing

**Purpose**: Verify that rollback to previous version works.

**Strategy**:
- Start with newer version
- Write test data
- Perform rolling downgrade to older version
- Verify data remains accessible
- Check for any incompatibilities

**Example**:
```java
@Test
public void testRollingDowngrade() throws Exception {
    String newerVersion = System.getenv("HADOOP_3_3_5_HOME");
    String olderVersion = System.getenv("HADOOP_3_3_1_HOME");

    // Start with newer version
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(newerVersion)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    // Write test data
    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Perform rolling downgrade
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, olderVersion, testFiles);

    // Verify data integrity
    UpgradeTestHelper.verifyTestData(fs, testFiles);

    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

### 4. Stress Testing During Upgrade

**Purpose**: Verify upgrade works under load.

**Strategy**:
- Start cluster
- Begin continuous read/write operations
- Perform upgrade while operations continue
- Verify no data loss or corruption

**Example**:
```java
@Test
public void testUpgradeUnderLoad() throws Exception {
    String sourceVersion = System.getenv("HADOOP_3_3_1_HOME");
    String targetVersion = System.getenv("HADOOP_3_3_5_HOME");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(sourceVersion)
        .format(true)
        .build();

    cluster.waitClusterUp();
    final FileSystem fs = cluster.getFileSystem();

    // Start background writer
    final AtomicBoolean running = new AtomicBoolean(true);
    final AtomicInteger filesWritten = new AtomicInteger(0);

    Thread writer = new Thread(() -> {
        while (running.get()) {
            try {
                Path file = new Path("/stress-test-" +
                    System.currentTimeMillis() + ".dat");
                FSDataOutputStream out = fs.create(file);
                out.write(new byte[1024 * 1024]);  // 1MB
                out.close();
                filesWritten.incrementAndGet();
                Thread.sleep(100);
            } catch (Exception e) {
                LOG.warn("Write operation failed during upgrade", e);
            }
        }
    });

    writer.start();

    try {
        // Perform upgrade while writes continue
        for (int i = 0; i < 3; i++) {
            cluster.shutdownDataNode(i);
            cluster.changeDataNodeVersion(i, targetVersion);
            cluster.startDataNode(i);
            cluster.waitClusterUp();
            Thread.sleep(2000);  // Let writes happen
        }

        LOG.info("Files written during upgrade: {}", filesWritten.get());
        assertTrue("Should have written files during upgrade",
            filesWritten.get() > 0);

    } finally {
        running.set(false);
        writer.join(5000);
    }
}
```

## Common Upgrade Scenarios

### Scenario 1: Patch Version Upgrade (3.3.1 → 3.3.5)

**Expected Result**: Should work seamlessly, no issues expected.

```java
@Test
public void testPatchVersionUpgrade() throws Exception {
    performBasicUpgrade("3.3.1", "3.3.5");
}

private void performBasicUpgrade(String from, String to) throws Exception {
    String fromHome = System.getenv("HADOOP_" +
        from.replace(".", "_") + "_HOME");
    String toHome = System.getenv("HADOOP_" +
        to.replace(".", "_") + "_HOME");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(fromHome)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, toHome, testFiles);
    UpgradeTestHelper.verifyTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

### Scenario 2: Minor Version Upgrade (3.3.x → 3.4.x)

**Expected Result**: Should work with potential warnings, test thoroughly.

```java
@Test
public void testMinorVersionUpgrade() throws Exception {
    String hadoop33 = System.getenv("HADOOP_3_3_5_HOME");
    String hadoop34 = System.getenv("HADOOP_3_4_0_HOME");

    Assume.assumeNotNull("HADOOP_3_4_0_HOME must be set", hadoop34);

    // Verify compatibility first
    VersionConfigAdapter v33 = new VersionConfigAdapter("3.3.5");
    VersionConfigAdapter v34 = new VersionConfigAdapter("3.4.0");
    assertTrue("Versions should be compatible",
        v33.isCompatibleWith(v34));

    // Perform upgrade
    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(hadoop33)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);
    UpgradeTestHelper.performRollingDataNodeUpgrade(cluster, hadoop34, testFiles);
    UpgradeTestHelper.verifyTestData(fs, testFiles);
    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

### Scenario 3: Partial Upgrade

**Purpose**: Test partially upgraded clusters (common in production).

```java
@Test
public void testPartialUpgrade() throws Exception {
    String sourceVersion = System.getenv("HADOOP_3_3_1_HOME");
    String targetVersion = System.getenv("HADOOP_3_3_5_HOME");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(4)
        .allNodesHadoopDistribution(sourceVersion)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Upgrade only 2 out of 4 DataNodes
    for (int i = 0; i < 2; i++) {
        cluster.shutdownDataNode(i);
        cluster.changeDataNodeVersion(i, targetVersion);
        cluster.startDataNode(i);
        cluster.waitClusterUp();
        UpgradeTestHelper.verifyTestData(fs, testFiles);
    }

    // Run for a while with mixed versions
    for (int cycle = 0; cycle < 5; cycle++) {
        List<Path> newFiles = UpgradeTestHelper.writeTestData(fs, 5);
        UpgradeTestHelper.verifyTestData(fs, newFiles);
        UpgradeTestHelper.cleanupTestData(fs, newFiles);
    }

    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

### Scenario 4: Multiple Successive Upgrades

**Purpose**: Test upgrading through multiple versions.

```java
@Test
public void testMultipleUpgrades() throws Exception {
    String v331 = System.getenv("HADOOP_3_3_1_HOME");
    String v333 = System.getenv("HADOOP_3_3_3_HOME");
    String v335 = System.getenv("HADOOP_3_3_5_HOME");
    String v336 = System.getenv("HADOOP_3_3_6_HOME");

    cluster = new ProcessBasedMiniDFSCluster.Builder(conf)
        .numDataNodes(3)
        .allNodesHadoopDistribution(v331)
        .format(true)
        .build();

    cluster.waitClusterUp();
    FileSystem fs = cluster.getFileSystem();

    List<Path> testFiles = UpgradeTestHelper.writeTestData(fs, 10);

    // Upgrade through versions
    String[] versions = {v333, v335, v336};
    for (String version : versions) {
        if (version != null) {
            LOG.info("Upgrading to {}", version);
            UpgradeTestHelper.performRollingDataNodeUpgrade(
                cluster, version, testFiles);
            UpgradeTestHelper.verifyTestData(fs, testFiles);
        }
    }

    UpgradeTestHelper.cleanupTestData(fs, testFiles);
}
```

## Version Compatibility Matrix

### Hadoop 3.x Series

| NN Version | DN Version | Status | Notes |
|------------|------------|--------|-------|
| 3.3.1      | 3.3.1      | ✅ Compatible | Baseline |
| 3.3.1      | 3.3.5      | ✅ Compatible | Forward compatible |
| 3.3.5      | 3.3.1      | ✅ Compatible | Backward compatible |
| 3.3.1      | 3.3.6      | ✅ Compatible | Forward compatible |
| 3.3.5      | 3.3.6      | ✅ Compatible | Adjacent patches |
| 3.3.x      | 3.4.x      | ⚠️ Mostly Compatible | Test thoroughly |
| 3.0.0      | 3.3.5      | ⚠️ Mostly Compatible | Large gap, test thoroughly |

### Hadoop 2.x Series

| NN Version | DN Version | Status | Notes |
|------------|------------|--------|-------|
| 2.10.1     | 2.10.2     | ✅ Compatible | Same minor version |
| 2.9.x      | 2.10.x     | ✅ Compatible | Adjacent minor versions |
| 2.7.x      | 2.10.x     | ⚠️ Risky | Large gap |

### Cross-Major Version

| NN Version | DN Version | Status | Notes |
|------------|------------|--------|-------|
| 2.x        | 3.x        | ❌ Incompatible | Protocol differences |
| 3.x        | 2.x        | ❌ Incompatible | Protocol differences |

### Testing Compatibility

```java
@Test
public void testVersionCompatibilityMatrix() {
    // Test compatible versions
    assertTrue(testCompatibility("3.3.1", "3.3.5"));
    assertTrue(testCompatibility("3.3.5", "3.3.1"));
    assertTrue(testCompatibility("3.3.1", "3.3.6"));

    // Test incompatible versions
    assertFalse(testCompatibility("2.10.2", "3.3.5"));
    assertFalse(testCompatibility("3.3.5", "2.10.2"));
}

private boolean testCompatibility(String v1, String v2) {
    return UpgradeTestHelper.verifyVersionCompatibility(v1, v2);
}
```

## Test Data Management

### Creating Test Data

```java
// Small dataset (10 files, 1MB each)
List<Path> smallDataset = UpgradeTestHelper.writeTestData(fs, 10);

// Medium dataset (50 files, 2MB each)
List<Path> mediumDataset = UpgradeTestHelper.writeTestData(fs, 50, 2 * 1024 * 1024);

// Large dataset (100 files, 10MB each)
List<Path> largeDataset = UpgradeTestHelper.writeTestData(fs, 100, 10 * 1024 * 1024);
```

### Verifying Data Integrity

```java
// Basic verification (files exist and readable)
UpgradeTestHelper.verifyTestData(fs, testFiles);

// Custom verification
for (Path file : testFiles) {
    assertTrue("File should exist", fs.exists(file));
    FileStatus stat = fs.getFileStatus(file);
    assertTrue("File should have content", stat.getLen() > 0);
}
```

### Cleanup

```java
// Cleanup test data
UpgradeTestHelper.cleanupTestData(fs, testFiles);

// Or manually
for (Path file : testFiles) {
    fs.delete(file, false);
}
fs.delete(new Path("/upgrade-test"), true);
```

## Troubleshooting Upgrades

### Common Issues

#### 1. DataNode Fails to Start After Upgrade

**Symptoms**: DataNode process exits immediately or fails to register.

**Diagnosis**:
```bash
# Check DataNode logs
tail -100 /tmp/process-minicluster-*/dn0/logs/*.log

# Look for:
# - ClassNotFoundException
# - Version mismatch errors
# - Configuration errors
```

**Solutions**:
```java
// Verify Hadoop distribution is valid
File hadoopHome = new File(targetVersion);
assertTrue("Hadoop home must exist", hadoopHome.exists());
assertTrue("Must have share/hadoop/hdfs",
    new File(hadoopHome, "share/hadoop/hdfs").exists());

// Check version compatibility
VersionConfigAdapter adapter = new VersionConfigAdapter(targetVersion);
assertTrue("Version must be valid", adapter.getMajorVersion() > 0);
```

#### 2. Data Loss After Upgrade

**Symptoms**: Files missing or corrupted after upgrade.

**Diagnosis**:
```java
// Check replication factor
FileStatus[] files = fs.listStatus(new Path("/"));
for (FileStatus file : files) {
    short replication = file.getReplication();
    LOG.info("File {} has replication {}", file.getPath(), replication);
}

// Check DataNode count
int dnCount = cluster.getNumDataNodes();
LOG.info("Active DataNodes: {}", dnCount);
```

**Prevention**:
```java
// Always verify data after each node upgrade
UpgradeTestHelper.performRollingDataNodeUpgrade(
    cluster,
    targetVersion,
    new UpgradeTestHelper.UpgradeVerifier() {
        @Override
        public void verify(ProcessBasedMiniDFSCluster cluster, int nodeIndex)
            throws Exception {
            FileSystem fs = cluster.getFileSystem();
            UpgradeTestHelper.verifyTestData(fs, testFiles);

            // Additional checks
            FileStatus[] status = fs.listStatus(new Path("/"));
            assertTrue("Should have test files", status.length > 0);
        }
    });
```

#### 3. Cluster Becomes Unstable

**Symptoms**: Intermittent errors, slow operations, nodes dropping out.

**Diagnosis**:
```java
// Monitor cluster health
while (upgrading) {
    try {
        UpgradeTestHelper.assertCanReadWriteData(fs);
        LOG.info("Cluster healthy");
    } catch (Exception e) {
        LOG.error("Cluster unhealthy", e);
    }
    Thread.sleep(5000);
}
```

**Solution**:
```java
// Wait longer between upgrades
for (int i = 0; i < numDataNodes; i++) {
    cluster.shutdownDataNode(i);
    cluster.changeDataNodeVersion(i, targetVersion);
    cluster.startDataNode(i);

    // Wait for full stabilization
    UpgradeTestHelper.waitForClusterStable(cluster, 60000);

    // Verify before proceeding
    UpgradeTestHelper.assertCanReadWriteData(fs);
}
```

## Best Practices

### 1. Always Verify Compatibility First

```java
@Before
public void verifyVersionCompatibility() {
    String sourceVersion = getSourceVersion();
    String targetVersion = getTargetVersion();

    boolean compatible = UpgradeTestHelper.verifyVersionCompatibility(
        sourceVersion, targetVersion);

    Assume.assumeTrue("Versions must be compatible", compatible);
}
```

### 2. Use Realistic Test Data

```java
// Mix of file sizes
List<Path> testFiles = new ArrayList<>();
testFiles.addAll(UpgradeTestHelper.writeTestData(fs, 10, 1024));        // 1KB files
testFiles.addAll(UpgradeTestHelper.writeTestData(fs, 10, 1024 * 1024)); // 1MB files
testFiles.addAll(UpgradeTestHelper.writeTestData(fs, 5, 10 * 1024 * 1024)); // 10MB files
```

### 3. Test Both Directions

```java
@Test
public void testUpgradeAndDowngrade() throws Exception {
    // Test upgrade
    performUpgrade(v1, v2);

    // Test downgrade
    performUpgrade(v2, v1);
}
```

### 4. Document Test Results

```java
@Test
public void testUpgrade() throws Exception {
    String source = System.getenv("HADOOP_3_3_1_HOME");
    String target = System.getenv("HADOOP_3_3_5_HOME");

    LOG.info("Testing upgrade from {} to {}", source, target);
    LOG.info("Test configuration: {} DataNodes, replication={}",
        numDataNodes, replication);

    // Perform upgrade
    long startTime = System.currentTimeMillis();
    performRollingUpgrade(source, target);
    long duration = System.currentTimeMillis() - startTime;

    LOG.info("Upgrade completed in {}ms", duration);
    LOG.info("Data integrity: VERIFIED");
    LOG.info("Cluster stability: STABLE");
}
```

### 5. Clean Up Thoroughly

```java
@After
public void tearDown() {
    try {
        if (cluster != null) {
            // Shutdown gracefully
            cluster.shutdown(true);
        }
    } catch (Exception e) {
        LOG.error("Error during cleanup", e);

        // Force cleanup
        try {
            ProcessBuilder pb = new ProcessBuilder("pkill", "-f", "ProcessLauncher");
            pb.start().waitFor();
        } catch (Exception ex) {
            LOG.error("Force cleanup failed", ex);
        }
    }
}
```

## Example: Complete Upgrade Test Suite

```java
public class ComprehensiveUpgradeTest {

    @Test
    public void testPatchUpgrade_3_3_1_to_3_3_5() throws Exception {
        runUpgradeTest("3.3.1", "3.3.5", true);
    }

    @Test
    public void testPatchDowngrade_3_3_5_to_3_3_1() throws Exception {
        runUpgradeTest("3.3.5", "3.3.1", true);
    }

    @Test
    public void testMinorUpgrade_3_3_to_3_4() throws Exception {
        runUpgradeTest("3.3.5", "3.4.0", true);
    }

    @Test
    public void testIncompatibleUpgrade_2_to_3() throws Exception {
        // Should fail or skip
        runUpgradeTest("2.10.2", "3.3.5", false);
    }

    private void runUpgradeTest(String fromVer, String toVer,
                                 boolean shouldSucceed) throws Exception {
        // Implementation here
    }
}
```

## Additional Resources

- [User Guide](ProcessBasedMiniDFSCluster-UserGuide.md)
- [Developer Guide](ProcessBasedMiniDFSCluster-DeveloperGuide.md)
- [Apache Hadoop Upgrade Documentation](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/HdfsRollingUpgrade.html)
