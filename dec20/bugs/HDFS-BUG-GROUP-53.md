# HDFS-BUG-GROUP-53: NPE in BlockManager.chooseExcessRedundancies After NameNode Restart Due to Stale Reference

## Summary

Restart-injected tests that store references to NameNode objects (`cluster.getNameNode(index)`) before a namenode restart fail with NullPointerException when those stale references are used after the restart. The `getBlockCollection()` method returns null for blocks when called on the old FSNamesystem, causing NPE when `bc.getStoragePolicyID()` is called.

## Classification

**TEST-BUG**

## Affected Tests

This issue affects 2 test executions:
- `TestDNFencing_RestartInjected.testDnFencing`
- `TestDNFencing_RestartInjected.testNNClearsCommandsOnFailoverAfterStartup`

## Stack Trace

```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.chooseExcessRedundancies(BlockManager.java:3940)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.processExtraRedundancyBlock(BlockManager.java:3924)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.setReplication(BlockManager.java:3884)
	at org.apache.hadoop.hdfs.server.namenode.FSDirAttrOp.unprotectedSetReplication(FSDirAttrOp.java:408)
	at org.apache.hadoop.hdfs.server.namenode.FSDirAttrOp.setReplication(FSDirAttrOp.java:144)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.setReplication(FSNamesystem.java:2377)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.setReplication(NameNodeRpcServer.java:858)
	at org.apache.hadoop.hdfs.server.namenode.ha.TestDNFencing_RestartInjected.testDnFencing(TestDNFencing_RestartInjected.java:152)
```

## Root Cause Analysis

### The Problem

The restart-injected test stores `nn1 = cluster.getNameNode(0)` in `@Before` setup. When the namenode is restarted, `cluster.getNameNode(0)` returns the NEW NameNode instance, but `nn1` still holds a reference to the OLD (closed) NameNode. When the test calls methods on this stale reference, it accesses the OLD FSNamesystem/FSDirectory which is in a closed/empty state.

### Code Flow

1. Test stores `nn1 = cluster.getNameNode(0)` at line 105 in `@Before`
2. Namenode restart is injected at lines 142-147 (`after_create_file`)
3. At line 152: `nn1.getRpcServer().setReplication(TEST_FILE, (short) 1)` uses the STALE reference
4. This is a **direct method call** (NOT an RPC call) on the OLD NameNode's RpcServer
5. The OLD FSNamesystem's `getBlockCollection(storedBlock)` returns `null` because the FSDirectory is closed/empty
6. NPE occurs at line 3940 when `bc.getStoragePolicyID()` is called on null `bc`

### Buggy Test Code Pattern

```java
// TestDNFencing_RestartInjected.java

public class TestDNFencing_RestartInjected {
    private NameNode nn1, nn2;  // Stored references

    @Before
    public void setupCluster() throws Exception {
        ...
        // Line 105: Store reference to namenode
        nn1 = cluster.getNameNode(0);
        nn2 = cluster.getNameNode(1);
        ...
    }

    @Test
    public void testDnFencing() throws Exception {
        // Line 139: Create file
        DFSTestUtil.createFile(fs, TEST_FILE_PATH, 30*SMALL_BLOCK, (short)3, 1L);

        // Lines 142-147: NAMENODE RESTART INJECTED HERE
        RestartFramework.at("after_create_file")
            .on(cluster)
            .restart("namenode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        // Line 152: Using STALE nn1 reference - causes NPE
        nn1.getRpcServer().setReplication(TEST_FILE, (short) 1);  // NPE HERE!
        ...
    }
}
```

### Source Code Reference

**BlockManager.java:3928-3946** - Where NPE occurs:
```java
private void chooseExcessRedundancies(
    final Collection<DatanodeStorageInfo> nonExcess,
    BlockInfo storedBlock, short replication,
    DatanodeDescriptor addedNode,
    DatanodeDescriptor delNodeHint) {
  assert namesystem.hasWriteLock();
  // first form a rack to datanodes map and
  BlockCollection bc = getBlockCollection(storedBlock);  // Returns NULL for stale FSNamesystem
  if (storedBlock.isStriped()) {
    chooseExcessRedundancyStriped(bc, nonExcess, storedBlock, delNodeHint);
  } else {
    final BlockStoragePolicy storagePolicy = storagePolicySuite.getPolicy(
        bc.getStoragePolicyID());  // NPE: bc is null!
    ...
  }
}
```

**FSNamesystem.java:3861-3864** - `getBlockCollection` returns null for closed FSDirectory:
```java
@Override
public INodeFile getBlockCollection(long id) {
  assert hasReadLock() : "Accessing INode id = " + id + " without read lock";
  INode inode = getFSDirectory().getInode(id);  // Returns null for closed/empty FSDirectory
  return inode == null ? null : inode.asFile();  // Returns null
}
```

## Fix and Verification

The restart-injected test needs to re-fetch the `nn1` and `nn2` references after namenode restart:

```java
// After restart, re-fetch the namenode reference
RestartFramework.at("after_create_file")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// FIX: Re-fetch references after namenode restart
nn1 = cluster.getNameNode(0);

nn1.getRpcServer().setReplication(TEST_FILE, (short) 1);
```

### General Pattern Fix

All restart-injected tests that restart the namenode should follow this pattern:
1. Before namenode restart, identify any direct references to namenode components
2. After namenode restart, re-fetch all those references
3. References that need refreshing include:
   - `cluster.getNameNode(index)`
   - `cluster.getNameNodeRpc()`
   - `cluster.getNamesystem()`
   - `cluster.getNamesystem().getBlockManager()`
   - Any other direct object references to namenode components

## Impact

This is not a production code bug. The production HDFS code handles namenode restarts through RPC with retry logic. The issue is that:
1. Tests use direct object references (bypassing RPC layer)
2. The test's `nn1` field holds a stale reference to the old NameNode object
3. The restart injection exposed the test's reliance on these direct references

## Production Code Improvement

While this is a TEST-BUG, the production code can be improved to provide better error messages instead of NPE. A defensive null check has been added to `BlockManager.chooseExcessRedundancies()` and `BlockManager.chooseExcessRedundancyStriped()`.

**Patch:** See [HDFS-XXXXX-improve-blockmanager-excess-redundancy-error-messages.patch](../patches/HDFS-XXXXX-improve-blockmanager-excess-redundancy-error-messages.patch)

The patch adds:
1. Null check for `BlockCollection bc` in `chooseExcessRedundancies()` - logs a warning and returns early instead of NPE
2. Defensive null check in `chooseExcessRedundancyStriped()` for safety

**Before (NPE):**
```java
BlockCollection bc = getBlockCollection(storedBlock);
// No null check - NPE if bc is null
final BlockStoragePolicy storagePolicy = storagePolicySuite.getPolicy(
    bc.getStoragePolicyID());
```

**After (Graceful handling):**
```java
BlockCollection bc = getBlockCollection(storedBlock);
if (bc == null) {
  LOG.warn("BLOCK* chooseExcessRedundancies: Cannot find BlockCollection "
      + "for block {}. The file may have been deleted or the NameNode "
      + "state is inconsistent. Skipping excess redundancy processing.",
      storedBlock);
  return;
}
// Safe to use bc now
final BlockStoragePolicy storagePolicy = storagePolicySuite.getPolicy(
    bc.getStoragePolicyID());
```

## Reproduction Steps

```bash
cd /home/shuai/xlab/restart_testing/hdfs/hadoop-hdfs-project/hadoop-hdfs
mvn surefire:test \
  -Dtest=TestDNFencing_RestartInjected#testDnFencing \
  -Drestart.position=after_create_file \
  -Drestart.target=namenode \
  -Drestart.mode=GRACEFUL
```

## Notes

This same pattern issue affects both test executions in this group:
1. `testDnFencing` - Uses stale `nn1` after restart at `after_create_file`
2. `testNNClearsCommandsOnFailoverAfterStartup` - Uses stale `nn1` after restart at `after_create_file_failover`

The common theme is identical to Groups 4, 19, 26, 44, and 48:
1. Test stores a reference to namenode in a field before restart
2. Namenode restart is injected
3. Test uses stale reference after restart
4. NPE occurs because the old namenode's internal state has been cleaned up
