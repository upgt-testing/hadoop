# TEST-BUG-GROUP-35: ReplicaNotFoundException due to MiniDFSCluster Index Mismatch After Restart

## Summary

A bug in `MiniDFSCluster.java` causes inconsistent behavior between `readBlockOnDataNode()` and `getMaterializedReplica()` methods after a datanode restart. The datanode list order changes after restart, but the storage directory mapping remains index-based, leading to `ReplicaNotFoundException`.

## Failing Test

- **Test Class**: `org.apache.hadoop.hdfs.server.blockmanagement.TestBlocksWithNotEnoughRacks_RestartInjected`
- **Test Method**: `testCorruptBlockRereplicatedAcrossRacks`
- **Restart Position**: `after_cluster_build`
- **Restart Target**: `datanode` (index 0)
- **Restart Mode**: `GRACEFUL`

## Exception

```
org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException: Replica not found for BP-...:blk_1073741825_1001
    at org.apache.hadoop.hdfs.server.datanode.fsdataset.impl.FsDatasetImplTestUtils.getMaterializedReplica(FsDatasetImplTestUtils.java:231)
    at org.apache.hadoop.hdfs.MiniDFSCluster.getMaterializedReplica(MiniDFSCluster.java:3193)
    at org.apache.hadoop.hdfs.MiniDFSCluster.corruptReplica(MiniDFSCluster.java:2373)
```

## Root Cause Analysis

### The Bug Location

The bug is in `MiniDFSCluster.java` in the test utilities.

### Mechanism

1. **`restartDataNode(int i)` changes list order**:
   - `stopDataNode(i)` removes the datanode from position `i` in the `dataNodes` list
   - `restartDataNode(DataNodeProperties)` appends the restarted datanode to the **END** of the list

   ```java
   // MiniDFSCluster.java:2587
   public synchronized boolean restartDataNode(int idn, ...) throws IOException {
       DataNodeProperties dnprop = stopDataNode(idn);  // Removes from position idn
       ...
       return restartDataNode(dnprop, keepPort);
   }

   // MiniDFSCluster.java:2533
   public synchronized boolean restartDataNode(DataNodeProperties dnprop, ...) {
       ...
       dataNodes.add(dnp);  // Appends to END, not at original position
       ...
   }
   ```

2. **`readBlockOnDataNode(int i)` uses index-based storage paths**:
   ```java
   // MiniDFSCluster.java:2344
   public String readBlockOnDataNode(int i, ExtendedBlock block) {
       File blockFile = getBlockFile(i, block);  // Uses index-based path
       ...
   }

   // MiniDFSCluster.java:3118
   public File getStorageDir(int dnIndex, int dirIndex) {
       return new File(determineDfsBaseDir(),
           getStorageDirPath(dnIndex, dirIndex));
   }

   // MiniDFSCluster.java:3130
   private String getStorageDirPath(int dnIndex, int dirIndex) {
       return "data/data" + (storagesPerDatanode * dnIndex + 1 + dirIndex);
   }
   ```

3. **`getMaterializedReplica(int i)` uses list-based DataNode lookup**:
   ```java
   // MiniDFSCluster.java:3191
   public MaterializedReplica getMaterializedReplica(int i, ExtendedBlock blk) {
       return getFsDatasetTestUtils(i).getMaterializedReplica(blk);
   }

   // MiniDFSCluster.java:2068
   public FsDatasetTestUtils getFsDatasetTestUtils(int dnIdx) {
       return FsDatasetTestUtils.Factory.getFactory(conf)
           .newInstance(dataNodes.get(dnIdx).datanode);  // Uses dataNodes list
   }
   ```

### Example Scenario

**Before restart** (4 datanodes):
- `dataNodes` list: `[host0, host1, host2, host3]`
- Storage mapping: `[data1-2, data3-4, data5-6, data7-8]`

**After restart of DN0**:
- `dataNodes` list: `[host1, host2, host3, host0']` (host0 moved to end)
- Storage mapping: **unchanged** (still index-based)

**The mismatch**:
- `readBlockOnDataNode(0, block)`: Checks `data1`, `data2` (host0's actual storage)
- `getMaterializedReplica(0, block)`: Queries `dataNodes.get(0)` = host1 (which uses `data3`, `data4`)

If a block is written to host0's storage (`data1`, `data2`):
- `readBlockOnDataNode(0, ...)` returns `HAS_BLOCK`
- `getMaterializedReplica(0, ...)` queries host1 and throws `ReplicaNotFoundException`

### Debug Evidence

From test run output:
```
DEBUG: DN[0] = host1.foo.com (UUID=be43c67c...)
DEBUG: DN[3] = host0.foo.com (UUID=6f09833b...)  // Restarted DN moved to end
DEBUG: readBlockOnDataNode checks storage at: data/data1  // host0's storage
DEBUG: getMaterializedReplica queries DN: host1.foo.com   // Different DN!
DEBUG: getStorageDir(0, 0) = .../data/data1  // Calculated from index
DEBUG: Expected storage for dataNodes[0] (host1.foo.com) = data1, data2  // Mismatch!
```

## Classification: TEST-BUG

This is a bug in the **test utility code** (`MiniDFSCluster.java`), not in production HDFS code:

1. `MiniDFSCluster` is test infrastructure
2. `readBlockOnDataNode()` and `getMaterializedReplica()` are test utility methods
3. The inconsistency only manifests when using these test utilities after a datanode restart

## Potential Fix

Option 1: **Insert at original position** (Recommended)
```java
// In restartDataNode(DataNodeProperties dnprop, boolean keepPort)
// Instead of: dataNodes.add(dnp);
// Use: dataNodes.add(originalIndex, dnp);
```

Option 2: **Fix readBlockOnDataNode to use DataNode's actual storage**
```java
public String readBlockOnDataNode(int i, ExtendedBlock block) {
    DataNode dn = dataNodes.get(i).datanode;
    // Get block file from dn's FsDataset instead of calculated path
}
```

Option 3: **Fix getStorageDir to account for list reordering**
```java
// Store original index in DataNodeProperties and use it for storage dir calculation
```

## Impact

Any test that:
1. Restarts a datanode using `MiniDFSCluster.restartDataNode()`
2. Then uses `readBlockOnDataNode()` to find blocks
3. Then uses `getMaterializedReplica()` or `corruptReplica()` on the same index

will potentially fail with `ReplicaNotFoundException` if the list order changed.
