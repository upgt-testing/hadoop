# FP-GROUP-3: StandbyException (Direct)

## Summary

**Verdict: FALSE POSITIVE**

The StandbyException failures in Group 3 are caused by the restart framework injecting restarts on the Active NameNode in an HA cluster without proper HA state restoration. After restart, the Active NameNode comes back as Standby (HDFS's default behavior), leaving the cluster without any Active NameNode. This is not a bug in HDFS but rather an issue with the restart framework's HA handling that has since been fixed.

## Exception Details

```
org.apache.hadoop.ipc.StandbyException: Operation category WRITE is not supported in state standby. Visit https://s.apache.org/sbnn-error
    at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java:108)
    at org.apache.hadoop.hdfs.server.namenode.NameNode$NameNodeHAContext.checkOperation(NameNode.java:2101)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkOperation(FSNamesystem.java:1585)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.mkdirs(FSNamesystem.java:3430)
    at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.mkdirs(NameNodeRpcServer.java:1166)
```

## Failure Pattern

**33 test failures** across multiple tests with similar patterns:
- Tests that use Observer/HA clusters (e.g., `TestStandbyInProgressTail`, `TestObserverNode`, `TestConsistentReadsObserver`)
- Restart positions that target the Active NameNode (index 0 in most cases)
- Failures occur during WRITE operations (like `mkdirs()`) after the restart

## Root Cause Analysis

### HA Cluster Setup
The affected tests use HA clusters where:
- **NN[0]**: ACTIVE (manually transitioned)
- **NN[1]**: STANDBY (default)
- **NN[2+]**: OBSERVER (for observer tests)

### What Happens During Restart (Original Issue)

1. **Restart Injection**: The restart framework triggers a restart at positions like `after_nn0_restart_and_active`, `after_mkdir_testpath`, etc., targeting the Active NameNode (index 0).

2. **NameNode Restart Behavior**: When `MiniDFSCluster.restartNameNode(0, true)` is called:
   ```java
   // From MiniDFSCluster.java:2282-2293
   shutdownNameNode(nnIndex);
   NameNode nn = NameNode.createNameNode(args, info.conf);
   info.nameNode = nn;
   ```
   The new NameNode starts in **STANDBY state** by default - this is normal HDFS behavior.

3. **Invalid Cluster State** (before fix): After restart, the cluster has:
   - **NN[0]**: STANDBY (was Active before restart, came back as Standby)
   - **NN[1]**: STANDBY
   - **NN[2]**: OBSERVER (if applicable)

   **No NameNode is Active!**

4. **WRITE Operation Failure**: When the test tries to perform a WRITE operation (like `mkdirs()`), the operation fails with StandbyException because there's no Active NameNode.

### The Fix

The restart framework has been updated to properly restore HA state after restart:

```java
// From HdfsClusterAdapter.java:253-293
private void restoreHAState(MiniDFSCluster cluster, int nnIndex, HAServiceState targetState) {
    try {
        NameNode nn = cluster.getNameNode(nnIndex);
        // ...
        switch (targetState) {
            case ACTIVE:
                LOG.info("Transitioning NameNode {} to ACTIVE", nnIndex);
                cluster.transitionToActive(nnIndex);
                cluster.waitActive(nnIndex);
                break;
            // ...
        }
        LOG.info("NameNode {} HA state restored to {}", nnIndex, targetState);
    } catch (Exception e) {
        // ...
    }
}
```

This fix:
1. Saves the HA state before restart
2. After the NameNode restarts (coming up as STANDBY by default)
3. Transitions the NameNode back to its previous state (ACTIVE)

### Why This Is NOT a Bug in HDFS

1. **Expected HDFS Behavior**: In HDFS HA, a restarted NameNode always comes up in STANDBY state. This is by design to prevent split-brain scenarios.

2. **Missing Automatic Failover**: Production HA clusters use ZKFC (ZooKeeper Failover Controller) which automatically promotes a Standby to Active when the Active fails. The test cluster doesn't have ZKFC.

3. **Framework Issue, Not HDFS Bug**: The issue was in the restart framework not handling HA state restoration properly. This has been fixed.

## Evidence from Reproduction

Multiple tests were executed to reproduce the failure:
```bash
mvn surefire:test -Dtest=org.apache.hadoop.hdfs.server.namenode.ha.TestStandbyInProgressTail_RestartInjected#testNonUniformConfig \
    -Drestart.position=after_nn0_restart_and_active -Drestart.target=namenode -Drestart.mode=GRACEFUL -Drestart.index=0
```

**Result: Tests now PASS** because the restart framework has been fixed to restore HA state.

Logs show the fix in action:
```
INFO  restart.HdfsClusterAdapter - NameNode 0 current HA state: standby, target state: active
INFO  restart.HdfsClusterAdapter - Transitioning NameNode 0 to ACTIVE
INFO  restart.HdfsClusterAdapter - NameNode 0 HA state restored to active
```

## Affected Tests (Sample)

| Test Class | Test Method | Restart Position | Target |
|-----------|------------|------------------|--------|
| TestStandbyInProgressTail_RestartInjected | testNonUniformConfig | after_nn0_restart_and_active | namenode:0 |
| TestObserverNode_RestartInjected | testDoubleFailover | after_mkdir_testpath | namenode:0 |
| TestConsistentReadsObserver_RestartInjected | testMsyncFileContext | after_msync_filecontext | namenode:0 |
| TestBootstrapStandby_RestartInjected | testSharedEditsMissingLogs | after_remove_standby_dirs | namenode:0 |
| TestCacheDirectives_RestartInjected | testExpiryTimeConsistency | after_modify_directive | namenode:0 |

## Difference from Group 2

Group 3 (StandbyException Direct) and Group 2 (StandbyException RemoteException) share the same root cause:
- **Group 2**: Exception thrown via RPC, wrapped in RemoteException
- **Group 3**: Exception thrown directly within the same JVM (during test code execution calling NameNode APIs directly)

Both are caused by the same HA state restoration issue in the restart framework.

## Conclusion

This is a **FALSE POSITIVE** because:

1. The StandbyException is the correct HDFS behavior when attempting WRITE operations on a non-Active NameNode.
2. The original issue was in the restart framework not restoring HA state after restart.
3. The restart framework has been fixed (`HdfsClusterAdapter.restoreHAState()`) to properly transition the NameNode back to ACTIVE after restart.
4. Tests now pass with the fix, confirming the issue was in the restart framework, not HDFS.

## Related

- **Group 2 (FP-GROUP-2.md)**: Same root cause, different exception wrapping
- **HdfsClusterAdapter.java:253-293**: HA state restoration fix
- **StandbyState.java:108**: Where StandbyException is thrown
