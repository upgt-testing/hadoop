# HDFS-BUG-GROUP-4: NPE in BlocksMap.getStoredBlock After NameNode Restart Due to Stale Reference

## Summary

Restart-injected tests that store references to NameNode RPC server (`cluster.getNameNodeRpc()`) before a namenode restart fail with NullPointerException when those stale references are used after the restart. The `BlocksMap.blocks` field becomes `null` after the old namenode shuts down, and subsequent calls to methods on the stale reference cause NPE.

## Classification

**TEST-BUG**

## Affected Tests

This issue affects 39 test executions across multiple test classes, including:
- `TestBlockManager_RestartInjected.testNeededReconstructionWhileAppending`
- `TestOverReplicatedBlocks_RestartInjected.testInvalidateOverReplicatedBlock`
- `TestProcessCorruptBlocks_RestartInjected.testWhenDecreasingReplication`
- `TestSnapshotDeletion_RestartInjected` (multiple methods)
- `TestNamenodeRetryCache_RestartInjected.testUpdatePipelineWithFailOver`
- `TestBlockUnderConstruction_RestartInjected.testGetBlockLocations`
- `TestUpdatePipelineWithSnapshots_RestartInjected.testUpdatePipelineAfterDelete`
- And more...

## Stack Trace

```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java:146)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.getStoredBlock(BlockManager.java:4625)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getStoredBlock(FSNamesystem.java:3820)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkUCBlock(FSNamesystem.java:5748)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.bumpBlockGenerationStamp(FSNamesystem.java:5822)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.updateBlockForPipeline(NameNodeRpcServer.java:985)
	at org.apache.hadoop.hdfs.server.blockmanagement.TestBlockManager_RestartInjected.testNeededReconstructionWhileAppending(TestBlockManager_RestartInjected.java:514)
```

## Root Cause Analysis

### The Problem

The restart-injected tests use `cluster.getNameNodeRpc()` to get a direct reference to the `NameNodeRpcServer` object. Unlike real RPC clients, this is a direct Java object reference. When the namenode restarts:

1. The old `NameNode` is shut down
2. `BlocksMap.close()` is called, which sets `blocks = null`:
   ```java
   // BlocksMap.java:94-96
   void close() {
     clear();
     blocks = null;  // <-- This sets blocks to null
   }
   ```
3. The new `NameNode` is created and started
4. The test still holds a stale reference to the **old** `NameNodeRpcServer`
5. Calling methods on this stale reference accesses the closed `BlocksMap` with `blocks = null`

### Buggy Test Code Pattern

```java
// TestBlockManager_RestartInjected.java
public void testNeededReconstructionWhileAppending() throws IOException {
    ...
    // Line 474: Get reference to namenode RPC server
    NamenodeProtocols namenode = cluster.getNameNodeRpc();
    ...
    // Line 505-506: Append to file
    namenode.append(src, clientName, new EnumSetWritable<>(
        EnumSet.of(CreateFlag.APPEND)));

    // Lines 507-512: NAMENODE RESTART INJECTED HERE
    RestartFramework.at("after_append")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    // Line 514: Using STALE reference - causes NPE
    LocatedBlock newLocatedBlock =
        namenode.updateBlockForPipeline(oldBlock, clientName);  // NPE HERE!
    ...
}
```

### Source Code Reference

**BlocksMap.java:94-96** - Where `blocks` is set to null:
```java
void close() {
  clear();
  blocks = null;
}
```

**BlocksMap.java:145-147** - Where NPE occurs:
```java
BlockInfo getStoredBlock(Block b) {
  return blocks.get(b);  // NPE if blocks is null
}
```

## Fix and Verification

The restart-injected test needs to re-fetch the `namenode` and `bm` (BlockManager) references after the namenode restart:

```java
// After restart, re-fetch the namenode reference
RestartFramework.at("after_append")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// FIX: Re-fetch references after namenode restart
namenode = cluster.getNameNodeRpc();
bm = cluster.getNamesystem().getBlockManager();

LocatedBlock newLocatedBlock =
    namenode.updateBlockForPipeline(oldBlock, clientName);
```

### Fix Verification

The fix was applied to `TestBlockManager_RestartInjected.testNeededReconstructionWhileAppending` and verified:

**Before fix:**
```
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java:146)
```

**After fix:**
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The fix was verified by running:
```bash
mvn surefire:test \
  -Dtest=TestBlockManager_RestartInjected#testNeededReconstructionWhileAppending \
  -Drestart.position=after_append \
  -Drestart.target=namenode \
  -Drestart.mode=GRACEFUL
```

### General Pattern Fix

All restart-injected tests that restart the namenode should follow this pattern:
1. Before namenode restart, identify any direct references to namenode components
2. After namenode restart, re-fetch all those references
3. References that need refreshing include:
   - `cluster.getNameNodeRpc()`
   - `cluster.getNamesystem()`
   - `cluster.getNamesystem().getBlockManager()`
   - `cluster.getNameNode()`
   - Any other direct object references to namenode components

## Impact

This is not a production code bug. The production HDFS code correctly closes resources on shutdown. The issue is that:
1. Tests use direct object references (bypassing RPC layer)
2. Real HDFS clients use RPC with retry logic that handles namenode restarts
3. The restart injection exposed the test's reliance on these direct references

## Reproduction Steps

```bash
cd /home/shuai/xlab/restart_testing/hdfs/hadoop-hdfs-project/hadoop-hdfs
mvn surefire:test \
  -Dtest=TestBlockManager_RestartInjected#testNeededReconstructionWhileAppending \
  -Drestart.position=after_append \
  -Drestart.target=namenode \
  -Drestart.mode=GRACEFUL
```

## Notes

This same pattern issue likely affects all 39 test executions in this group. The common theme is:
1. Test gets a reference to namenode/namesystem/blockmanager before restart
2. Namenode restart is injected
3. Test uses stale reference after restart
4. NPE occurs because the old namenode's internal state has been cleaned up
