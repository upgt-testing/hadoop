# HDFS-BUG-GROUP-90: NPE in DFSTestUtil.waitForDatanodeState After NameNode Restart

## Summary
NullPointerException occurs in `DFSTestUtil.waitForDatanodeState()` when called with a datanode UUID that was obtained before a NameNode restart. After restart, the new NameNode's DatanodeManager doesn't have the old datanode registered, causing a null DatanodeDescriptor to be returned.

## Classification
**TEST-BUG** - The restart-injected test uses stale datanode registration information after NameNode restart.

## Affected Test
- `org.apache.hadoop.hdfs.server.namenode.TestDeadDatanode_RestartInjected.testDeadNodeAsBlockTarget`
- Restart Position: `after_datanode_shutdown_test2`
- Restart Target: `namenode`
- Restart Mode: `GRACEFUL`

## Stack Trace
```
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.DFSTestUtil$5.get(DFSTestUtil.java:2010)
    at org.apache.hadoop.hdfs.DFSTestUtil$5.get(DFSTestUtil.java:2004)
    at org.apache.hadoop.test.GenericTestUtils.waitFor(GenericTestUtils.java:449)
    at org.apache.hadoop.test.GenericTestUtils.waitFor(GenericTestUtils.java:421)
    at org.apache.hadoop.hdfs.DFSTestUtil.waitForDatanodeState(DFSTestUtil.java:2004)
    at org.apache.hadoop.hdfs.server.namenode.TestDeadDatanode_RestartInjected.testDeadNodeAsBlockTarget(TestDeadDatanode_RestartInjected.java:237)
```

## Root Cause Analysis

### Test Flow Leading to Failure
1. **Line 212-213**: Test obtains datanode registration before restart:
   ```java
   DatanodeRegistration reg = InternalDataNodeTestUtils.
       getDNRegistrationForBP(cluster.getDataNodes().get(0), poolId);
   ```

2. **Line 229**: Datanode is shut down:
   ```java
   dn.shutdown();
   ```

3. **Lines 230-235**: NameNode restart is injected at `after_datanode_shutdown_test2`:
   ```java
   RestartFramework.at("after_datanode_shutdown_test2")
       .on(cluster)
       .restart("namenode")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();
   ```

4. **Line 237-238**: Test calls `waitForDatanodeState` with old UUID:
   ```java
   DFSTestUtil.waitForDatanodeState(cluster, reg.getDatanodeUuid(), false, 20000);
   ```

### Why NPE Occurs

Inside `DFSTestUtil.waitForDatanodeState` (lines 2004-2012):
```java
public static void waitForDatanodeState(
    final MiniDFSCluster cluster, final String nodeID,
    final boolean alive, int waitTime)
    throws TimeoutException, InterruptedException {
  GenericTestUtils.waitFor(new Supplier<Boolean>() {
    @Override
    public Boolean get() {
      FSNamesystem namesystem = cluster.getNamesystem();  // Gets NEW namesystem
      final DatanodeDescriptor dd = BlockManagerTestUtil.getDatanode(
          namesystem, nodeID);  // nodeID is OLD datanode UUID
      return (dd.isAlive() == alive);  // dd is NULL -> NPE!
    }
  }, 100, waitTime);
}
```

After NameNode restart:
- `cluster.getNamesystem()` returns the NEW FSNamesystem
- The NEW FSNamesystem's DatanodeManager has an empty `datanodeMap`
- The datanode was shut down before restart and won't re-register
- `getDatanode(nodeID)` returns `null` because the UUID doesn't exist
- `dd.isAlive()` is called on `null`, causing NPE

### DatanodeManager.getDatanode() Returns Null
```java
// DatanodeManager.java:746-753
public DatanodeDescriptor getDatanode(final String datanodeUuid) {
  if (datanodeUuid == null) {
    return null;
  }
  synchronized (this) {
    return datanodeMap.get(datanodeUuid);  // Returns null if not found
  }
}
```

## Buggy Code Location

### Test Utility Code (DFSTestUtil.java)
**File:** `hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/DFSTestUtil.java`
**Lines:** 2004-2012

```java
public static void waitForDatanodeState(
    final MiniDFSCluster cluster, final String nodeID,
    final boolean alive, int waitTime)
    throws TimeoutException, InterruptedException {
  GenericTestUtils.waitFor(new Supplier<Boolean>() {
    @Override
    public Boolean get() {
      FSNamesystem namesystem = cluster.getNamesystem();
      final DatanodeDescriptor dd = BlockManagerTestUtil.getDatanode(
          namesystem, nodeID);
      return (dd.isAlive() == alive);  // No null check on dd!
    }
  }, 100, waitTime);
}
```

## Potential Fix

### Option 1: Add Null Check in DFSTestUtil.waitForDatanodeState
```java
public static void waitForDatanodeState(
    final MiniDFSCluster cluster, final String nodeID,
    final boolean alive, int waitTime)
    throws TimeoutException, InterruptedException {
  GenericTestUtils.waitFor(new Supplier<Boolean>() {
    @Override
    public Boolean get() {
      FSNamesystem namesystem = cluster.getNamesystem();
      final DatanodeDescriptor dd = BlockManagerTestUtil.getDatanode(
          namesystem, nodeID);
      if (dd == null) {
        // If datanode doesn't exist in the NameNode:
        // - Return true if waiting for dead state (non-existent = effectively dead)
        // - Return false if waiting for alive state
        return !alive;
      }
      return (dd.isAlive() == alive);
    }
  }, 100, waitTime);
}
```

### Option 2: Fix the Restart-Injected Test
After namenode restart, the test scenario becomes invalid because:
1. The datanode was shut down before restart
2. The new NameNode has no knowledge of the datanode
3. Waiting for "dead" state is meaningless when the datanode never existed

The restart at `after_datanode_shutdown_test2` should either:
- Not be triggered (mark as an invalid restart position), OR
- The test should be modified to handle the restart scenario properly

## Impact
- **Severity:** Low (Test utility issue only)
- **Production Impact:** None - this is purely a test infrastructure issue
- **Scope:** Affects restart-injected tests that use `DFSTestUtil.waitForDatanodeState` after NameNode restart

## Related Issues
This follows the same pattern as Groups 4, 19, 26, 44, 48, 53, and 58 where tests hold stale references to NameNode components that become invalid after restart.
