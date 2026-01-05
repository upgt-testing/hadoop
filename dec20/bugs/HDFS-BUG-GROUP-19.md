# HDFS-BUG-GROUP-19: NPE in BlocksMap.numNodes After NameNode Restart Due to Stale Reference

## Summary

Restart-injected tests that store references to NameNode RPC server (`cluster.getNameNodeRpc()`) in `@Before` setup before a namenode restart fail with NullPointerException when those stale references are used after the restart. The `BlocksMap.blocks` field becomes `null` after the old namenode shuts down, and subsequent calls to `numNodes()` on the stale reference cause NPE.

## Classification

**TEST-BUG**

## Affected Tests

This issue affects 8 test executions in `TestHDFSConcat_RestartInjected`:
- `testConcatNotCompleteBlock` (positions: before_concat, after_concat)
- `testConcat` (position: after_source_files_creation)
- And other test methods with namenode restart positions

## Stack Trace

```
java.lang.NullPointerException
	at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.numNodes(BlocksMap.java:172)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlock(BlockManager.java:1420)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlock(BlockManager.java:1382)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlockList(BlockManager.java:1353)
	at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.createLocatedBlocks(BlockManager.java:1503)
	at org.apache.hadoop.hdfs.server.namenode.FSDirStatAndListingOp.getBlockLocations(FSDirStatAndListingOp.java:179)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getBlockLocations(FSNamesystem.java:2124)
	at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.getBlockLocations(NameNodeRpcServer.java:769)
	at org.apache.hadoop.hdfs.server.namenode.TestHDFSConcat_RestartInjected.testConcatNotCompleteBlock(TestHDFSConcat_RestartInjected.java:408)
```

## Root Cause Analysis

### The Problem

The test class stores `nn = cluster.getNameNodeRpc()` in the `@Before` setup method. This is a direct Java object reference (not a real RPC client). When the namenode restarts:

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
4. The test still holds a stale reference to the **old** `NameNodeRpcServer` via `nn`
5. Calling methods on this stale reference accesses the closed `BlocksMap` with `blocks = null`

### Buggy Test Code Pattern

```java
// TestHDFSConcat_RestartInjected.java

// Line 63: Field declaration
private NamenodeProtocols nn;

// Lines 76-85: Setup stores reference ONCE
@Before
public void startUpCluster() throws IOException {
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPL_FACTOR).build();
    ...
    nn = cluster.getNameNodeRpc();  // <-- Reference stored here
}

// Lines 341-425: Test method
@Test
public void testConcatNotCompleteBlock() throws IOException {
    ...
    // Lines 352-376: Using nn before restart - works fine
    HdfsFileStatus fStatus = nn.getFileInfo(name1);
    LocatedBlocks lb1 = nn.getBlockLocations(name1, 0, trgFileLen);
    ...

    // Lines 381-386: NAMENODE RESTART INJECTED HERE
    RestartFramework.at("before_concat")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    dfs.concat(filePath1, new Path [] {filePath2});  // Works - DFS reconnects

    ...
    // Line 408: Using STALE reference - causes NPE
    LocatedBlocks lbConcat = nn.getBlockLocations(name1, 0, fileLen);  // NPE HERE!
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

**BlocksMap.java:171-173** - Where NPE occurs:
```java
int numNodes(Block b) {
  BlockInfo info = blocks.get(b);  // NPE if blocks is null
  return info == null ? 0 : info.numNodes();
}
```

## Fix

The restart-injected test needs to re-fetch the `nn` reference after each namenode restart:

```java
// After restart, re-fetch the namenode reference
RestartFramework.at("before_concat")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// FIX: Re-fetch nn reference after namenode restart
nn = cluster.getNameNodeRpc();

dfs.concat(filePath1, new Path [] {filePath2});
...
LocatedBlocks lbConcat = nn.getBlockLocations(name1, 0, fileLen);  // Now works
```

### Fix Verification

The fix was applied to all restart points in `TestHDFSConcat_RestartInjected` and verified:

**Before fix:**
```
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.numNodes(BlocksMap.java:172)
```

**After fix:**
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Verified with all three positions:
- `testConcatNotCompleteBlock` with `before_concat`: PASS
- `testConcatNotCompleteBlock` with `after_concat`: PASS
- `testConcat` with `after_source_files_creation`: PASS

## Relation to Group 4

This issue is identical in nature to Group 4 (HDFS-BUG-GROUP-4.md). Both are TEST-BUG caused by:
1. Tests storing direct references to namenode components before restart
2. Not refreshing those references after namenode restart
3. Using stale references to access closed `BlocksMap` where `blocks = null`

The difference is only in the specific method that throws NPE:
- Group 4: `BlocksMap.getStoredBlock()` at line 146
- Group 19: `BlocksMap.numNodes()` at line 172

## Impact

This is not a production code bug. The production HDFS code correctly closes resources on shutdown. The issue is that:
1. Tests use direct object references (bypassing RPC layer)
2. Real HDFS clients use RPC with retry logic that handles namenode restarts
3. The restart injection exposed the test's reliance on these direct references

## Reproduction Steps

```bash
cd /home/shuai/xlab/restart_testing/hdfs/hadoop-hdfs-project/hadoop-hdfs
mvn surefire:test \
  -Dtest=TestHDFSConcat_RestartInjected#testConcatNotCompleteBlock \
  -Drestart.position=before_concat \
  -Drestart.target=namenode \
  -Drestart.mode=GRACEFUL
```

## Notes

All 8 test executions in this group have the same root cause - the `nn` field reference stored in `@Before` becomes stale after any namenode restart is injected. The fix applies to all namenode restart positions in `TestHDFSConcat_RestartInjected`.
