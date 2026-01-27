# HDFS-XXXXX: MiniDFSCluster.restartDataNode throws ArrayIndexOutOfBoundsException when DataNode was added without storageCapacities

## Summary

`MiniDFSCluster.restartDataNode()` throws `ArrayIndexOutOfBoundsException` when attempting to restart a DataNode that was added to the cluster without specifying storage capacities.

## Component

- **Component**: hdfs, test

## Affects Version/s

- 3.3.5

## Priority

- Major

## Description

When a DataNode is added to a MiniDFSCluster using `startDataNodes()` with `storageCapacities = null`, and later that DataNode is restarted using `restartDataNode()`, an `ArrayIndexOutOfBoundsException` is thrown.

The root cause is that `MiniDFSCluster.setDataNodeStorageCapacities()` does not validate that the DataNode index is within the bounds of the `storageCapacities` array before accessing it.

### Steps to Reproduce

1. Create a MiniDFSCluster with explicit storage capacities:
   ```java
   cluster = new MiniDFSCluster.Builder(conf)
       .numDataNodes(1)
       .storageCapacities(new long[] { CAPACITY })
       .build();
   ```

2. Add another DataNode without specifying storage capacities:
   ```java
   cluster.startDataNodes(conf, 1, true, null, null);  // storageCapacities is null
   ```

3. Restart the second DataNode:
   ```java
   cluster.restartDataNode(1);  // Throws ArrayIndexOutOfBoundsException
   ```

### Expected Behavior

`restartDataNode()` should successfully restart the DataNode regardless of whether storage capacities were specified when the DataNode was originally added.

### Actual Behavior

```
java.lang.ArrayIndexOutOfBoundsException: 1
    at org.apache.hadoop.hdfs.MiniDFSCluster.setDataNodeStorageCapacities(MiniDFSCluster.java:1882)
    at org.apache.hadoop.hdfs.MiniDFSCluster.restartDataNode(MiniDFSCluster.java:2557)
    at org.apache.hadoop.hdfs.MiniDFSCluster.restartDataNode(MiniDFSCluster.java:2596)
    at org.apache.hadoop.hdfs.MiniDFSCluster.restartDataNode(MiniDFSCluster.java:2576)
```

## Root Cause Analysis

The issue is in `MiniDFSCluster.java`:

1. **In `startDataNodes()`** (lines 1847-1849): Storage capacities are only added to `storageCap` list when they are explicitly provided:
   ```java
   /* memorize storage capacities */
   if (storageCapacities != null) {
     storageCap.addAll(Arrays.asList(storageCapacities));
   }
   ```
   When `storageCapacities` is `null`, no entry is added to `storageCap`.

2. **In `restartDataNode()`** (lines 2557-2560): The method passes the DataNode's index and the entire `storageCap` array:
   ```java
   setDataNodeStorageCapacities(
       dataNodes.lastIndexOf(dnp),
       newDn,
       storageCap.toArray(new long[][]{}));
   ```

3. **In `setDataNodeStorageCapacities()`** (lines 1865-1894): The method checks for null/empty array but does NOT check if the index is within bounds:
   ```java
   private synchronized void setDataNodeStorageCapacities(
       final int curDnIdx,
       final DataNode curDn,
       long[][] storageCapacities) throws IOException {

     if (storageCapacities == null || storageCapacities.length == 0) {
       return;
     }
     // ...
     // BUG: No bounds check for curDnIdx!
     assert storageCapacities[curDnIdx].length == storagesPerDatanode;  // CRASH!
   }
   ```

## Proposed Fix

Add a bounds check in `setDataNodeStorageCapacities()` to handle the case where the DataNode index exceeds the storage capacities array length.

### Patch

```diff
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/MiniDFSCluster.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/MiniDFSCluster.java
index abcdef1..1234567 100644
--- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/MiniDFSCluster.java
+++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/MiniDFSCluster.java
@@ -1865,7 +1865,10 @@ public class MiniDFSCluster implements AutoCloseable {
   private synchronized void setDataNodeStorageCapacities(
       final int curDnIdx,
       final DataNode curDn,
       long[][] storageCapacities) throws IOException {

-    if (storageCapacities == null || storageCapacities.length == 0) {
+    // Check for null/empty array AND ensure index is within bounds.
+    // DataNodes added without explicit storageCapacities won't have
+    // an entry in the storageCap list.
+    if (storageCapacities == null || storageCapacities.length == 0
+        || curDnIdx >= storageCapacities.length) {
       return;
     }
```

### Full Fixed Method

```java
private synchronized void setDataNodeStorageCapacities(
    final int curDnIdx,
    final DataNode curDn,
    long[][] storageCapacities) throws IOException {

  // Check for null/empty array AND ensure index is within bounds.
  // DataNodes added without explicit storageCapacities won't have
  // an entry in the storageCap list.
  if (storageCapacities == null || storageCapacities.length == 0
      || curDnIdx >= storageCapacities.length) {
    return;
  }

  try {
    waitDataNodeFullyStarted(curDn);
  } catch (TimeoutException | InterruptedException e) {
    throw new IOException(e);
  }

  try (FsDatasetSpi.FsVolumeReferences volumes = curDn.getFSDataset()
      .getFsVolumeReferences()) {
    assert storageCapacities[curDnIdx].length == storagesPerDatanode;
    assert volumes.size() == storagesPerDatanode;

    int j = 0;
    for (FsVolumeSpi fvs : volumes) {
      FsVolumeImpl volume = (FsVolumeImpl) fvs;
      LOG.info("setCapacityForTesting " + storageCapacities[curDnIdx][j]
          + " for [" + volume.getStorageType() + "]" + volume.getStorageID());
      volume.setCapacityForTesting(storageCapacities[curDnIdx][j]);
      j++;
    }
  }
  DataNodeTestUtils.triggerHeartbeat(curDn);
}
```

## Test Case

The following test case reproduces the issue:

```java
@Test
public void testRestartDataNodeWithoutStorageCapacities() throws Exception {
  Configuration conf = new HdfsConfiguration();

  // Create cluster with 1 DN with explicit storage capacities
  try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf)
      .numDataNodes(1)
      .storageCapacities(new long[] { 1024 * 1024 * 10 })
      .storagesPerDatanode(1)
      .build()) {

    cluster.waitActive();

    // Add another DN without storage capacities
    cluster.startDataNodes(conf, 1, true, null, null);
    cluster.waitActive();

    // This should not throw ArrayIndexOutOfBoundsException
    cluster.restartDataNode(1);
    cluster.waitActive();

    assertEquals(2, cluster.getDataNodes().size());
  }
}
```

## Attachments

- Stack trace from failed test execution
- Analysis performed during restart testing framework validation

## Labels

- test-infrastructure
- mini-cluster
