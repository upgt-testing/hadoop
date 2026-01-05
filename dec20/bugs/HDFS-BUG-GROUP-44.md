# HDFS-BUG-GROUP-44: NullPointerException in BlocksMap.removeNode Due to Stale FSNamesystem Reference After Namenode Restart

## Summary

**Classification:** TEST-BUG

Test stores references to NameNode components (`FSNamesystem`, `DatanodeManager`, `BlockManager`) before namenode restart. After restart, the test uses these stale references which point to closed objects, causing NullPointerException when accessing `BlocksMap.blocks` which has been set to `null` during shutdown.

## Affected Tests

1. `org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionStatusAfterDNRestart`
   - Position: after_datanode_stopped, Target: namenode, Mode: GRACEFUL

2. `org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyIsHot_RestartInjected.testDatanodeRestarts`
   - Position: after_file_creation_and_standby_catchup, Target: namenode, Mode: GRACEFUL

## Stack Trace

```
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.removeNode(BlocksMap.java:182)
    at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.removeStoredBlock(BlockManager.java:4098)
    at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.removeBlocksAssociatedTo(BlockManager.java:1688)
    at org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager.removeDatanode(DatanodeManager.java:838)
    at org.apache.hadoop.hdfs.server.blockmanagement.DatanodeManager.removeDeadDatanode(DatanodeManager.java:883)
    at org.apache.hadoop.hdfs.server.blockmanagement.HeartbeatManager.heartbeatCheck(HeartbeatManager.java:498)
    at org.apache.hadoop.hdfs.server.blockmanagement.BlockManagerTestUtil.checkHeartbeat(BlockManagerTestUtil.java:292)
    at org.apache.hadoop.hdfs.server.namenode.TestDecommissioningStatus_RestartInjected.testDecommissionStatusAfterDNRestart(TestDecommissioningStatus_RestartInjected.java:493)
```

## Root Cause Analysis

### Test Code (Buggy)

In `TestDecommissioningStatus_RestartInjected.testDecommissionStatusAfterDNRestart()`:

```java
// Lines 458-459: References stored BEFORE namenode restart
FSNamesystem fsn = cluster.getNamesystem();
final DatanodeManager dm = fsn.getBlockManager().getDatanodeManager();
decommissionNode(dnName);
dm.refreshNodes(conf);

// ... some test logic ...

// Lines 485-490: Namenode is restarted
RestartFramework.at("after_datanode_stopped")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// Line 493: PROBLEM - Using stale 'fsn' reference after restart
BlockManagerTestUtil.checkHeartbeat(fsn.getBlockManager());

// Line 496: Also problematic - using stale 'dm' reference
BlockManagerTestUtil.recheckDecommissionState(dm);
```

### Production Code (Not Buggy)

The `BlocksMap.close()` method correctly nullifies the `blocks` field during shutdown:

```java
// BlocksMap.java:94-97
void close() {
    clear();
    blocks = null;  // <-- This is correct shutdown behavior
}
```

And `BlocksMap.removeNode()` at line 182:

```java
boolean removeNode(Block b, DatanodeDescriptor node) {
    BlockInfo info = blocks.get(b);  // <-- NPE when blocks is null
    if (info == null)
        return false;
    // ...
}
```

### What Happens

1. Test stores `fsn = cluster.getNamesystem()` at line 458
2. Test stores `dm = fsn.getBlockManager().getDatanodeManager()` at line 459
3. Namenode is restarted at lines 485-490
4. During restart, the OLD FSNamesystem is closed:
   - `FSNamesystem.close()` -> `BlockManager.close()` -> `BlocksMap.close()`
   - `BlocksMap.close()` sets `blocks = null`
5. After restart, `cluster.getNamesystem()` returns a NEW FSNamesystem
6. But the test still uses the OLD `fsn` reference at line 493
7. `fsn.getBlockManager()` returns the OLD (closed) BlockManager
8. When `checkHeartbeat()` eventually calls `BlocksMap.removeNode()`, it accesses `blocks.get(b)` on the closed BlocksMap where `blocks = null`
9. **NPE at line 182**

## Proposed Fix

After any namenode restart in the test, re-fetch all direct references to NameNode components:

```java
RestartFramework.at("after_datanode_stopped")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// FIX: Re-fetch references after namenode restart
fsn = cluster.getNamesystem();
dm = fsn.getBlockManager().getDatanodeManager();

// Now safe to use the new references
BlockManagerTestUtil.checkHeartbeat(fsn.getBlockManager());
BlockManagerTestUtil.recheckDecommissionState(dm);
```

## Related Issues

This is the same pattern as:
- **Group 4**: NPE in `BlocksMap.getStoredBlock()` - stale NameNode references after restart
- **Group 19**: NPE in `BlocksMap.numNodes()` - stale `nn` reference after restart

## Classification Rationale

This is classified as **TEST-BUG** because:

1. The production code (`BlocksMap.close()`) is behaving correctly by cleaning up resources during shutdown
2. The test code was not designed for restart scenarios - it assumes NameNode references remain valid throughout the test
3. The restart-injected version of the test needs to be adapted to re-fetch references after restart
4. There is no production code defect - the issue is purely in how the test interacts with the restart framework
