# HDFS-BUG-GROUP-8: IllegalStateException in FSEditLog.getCurSegmentTxId After NameNode Restart Due to Stale Reference

## Summary

Restart-injected tests that store references to NameNode (`cluster.getNameNode()`) before a namenode restart fail with IllegalStateException when those stale references are used after the restart. The old FSEditLog's state becomes `CLOSED` after the namenode shuts down, and subsequent calls to methods like `getCurSegmentTxId()` on the stale reference cause IllegalStateException because the method requires the edit log segment to be open.

## Classification

**TEST-BUG**

## Affected Tests

This issue affects 28 test executions, primarily:
- `TestBackupNode_RestartInjected.testBackupNodeTailsEdits` (at positions: after_edit_log_roll, after_third_sync, after_bn_stop, etc.)
- `TestBackupNode_RestartInjected.testCheckpoint` (at positions: after_cluster_start, after_mkdir_file1, after_backup_start, etc.)
- `TestBackupNode_RestartInjected.testCanReadData` (at various positions)

## Stack Trace

```
java.lang.IllegalStateException: Bad state: CLOSED
	at org.apache.hadoop.thirdparty.com.google.common.base.Preconditions.checkState(Preconditions.java:591)
	at org.apache.hadoop.hdfs.server.namenode.FSEditLog.getCurSegmentTxId(FSEditLog.java:578)
	at org.apache.hadoop.hdfs.server.namenode.TestBackupNode_RestartInjected.testBackupNodeTailsEdits(TestBackupNode_RestartInjected.java:263)
```

## Root Cause Analysis

### The Problem

The restart-injected test uses `cluster.getNameNode()` to get a direct reference to the `NameNode` object. Unlike real RPC clients, this is a direct Java object reference. When the namenode restarts:

1. The old `NameNode` is shut down
2. `FSEditLog.close()` is called, which sets the state to `CLOSED`:
   ```java
   // FSEditLog.java:414
   state = State.CLOSED;
   ```
3. The new `NameNode` is created and started
4. The test still holds a stale reference to the **old** `NameNode`
5. Calling `getCurSegmentTxId()` on the stale reference accesses the closed `FSEditLog`

### Buggy Test Code Pattern

```java
// TestBackupNode_RestartInjected.java
public void testBackupNodeTailsEdits() throws Exception {
    ...
    // Line 253: Get reference to namenode
    NameNode nn = cluster.getNameNode();
    NamenodeProtocols nnRpc = nn.getRpcServer();
    nnRpc.rollEditLog();

    // Lines 256-261: NAMENODE RESTART INJECTED HERE
    RestartFramework.at("after_edit_log_roll")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    // Lines 262-263: Using STALE reference - causes IllegalStateException
    assertEquals(bnImage.getEditLog().getCurSegmentTxId(),
        nn.getFSImage().getEditLog().getCurSegmentTxId());  // IllegalStateException HERE!
    ...
}
```

### Source Code Reference

**FSEditLog.java:414** - Where state is set to CLOSED:
```java
state = State.CLOSED;
```

**FSEditLog.java:577-580** - Where IllegalStateException occurs:
```java
public synchronized long getCurSegmentTxId() {
  Preconditions.checkState(isSegmentOpen(),
      "Bad state: %s", state);  // Throws IllegalStateException if state is CLOSED
  return curSegmentTxId;
}
```

**FSEditLog.java:368-369** - The isSegmentOpen() check:
```java
synchronized boolean isSegmentOpen() {
  return state == State.IN_SEGMENT;
}
```

## Fix and Verification

The restart-injected test needs to re-fetch the `nn` reference after the namenode restart:

```java
// After restart, re-fetch the namenode reference
RestartFramework.at("after_edit_log_roll")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// FIX: Re-fetch references after namenode restart
nn = cluster.getNameNode();

assertEquals(bnImage.getEditLog().getCurSegmentTxId(),
    nn.getFSImage().getEditLog().getCurSegmentTxId());
```

### General Pattern Fix

All restart-injected tests that restart the namenode should follow this pattern:
1. Before namenode restart, identify any direct references to namenode components
2. After namenode restart, re-fetch all those references
3. References that need refreshing include:
   - `cluster.getNameNode()`
   - `cluster.getNameNodeRpc()`
   - `cluster.getNamesystem()`
   - `cluster.getNamesystem().getBlockManager()`
   - `nn.getFSImage().getEditLog()`
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
  -Dtest=TestBackupNode_RestartInjected#testBackupNodeTailsEdits \
  -Drestart.position=after_edit_log_roll \
  -Drestart.target=namenode \
  -Drestart.mode=GRACEFUL
```

## Notes

This same pattern issue affects all 28 test executions in this group. The common theme is:
1. Test gets a reference to NameNode/FSImage/FSEditLog before restart
2. Namenode restart is injected
3. Test uses stale reference after restart
4. IllegalStateException occurs because the old namenode's FSEditLog state has been set to CLOSED

This is the same category of issue as Groups 4, 19, 26, 44, 48, 53, 58, 90, 93, and 95 - all of which involve stale references to namenode components after restart.
