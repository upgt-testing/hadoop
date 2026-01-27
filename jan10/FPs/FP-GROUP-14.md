# FP-GROUP-14: StandbyException (Caused by RemoteException)

## Summary

**Verdict: FALSE POSITIVE**

The StandbyException failures in Group 14 are caused by the same root issue as Groups 2 and 3: the restart framework was injecting restarts on the Active NameNode in an HA cluster without proper HA state restoration. After restart, the Active NameNode comes back as Standby (HDFS's default behavior), leaving the cluster without any Active NameNode. This is not a bug in HDFS but rather an issue with the restart framework's HA handling that has since been fixed.

## Exception Details

```
Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException):
Operation category WRITE is not supported in state standby. Visit https://s.apache.org/sbnn-error
    at org.apache.hadoop.hdfs.server.namenode.ha.StandbyState.checkOperation(StandbyState.java:108)
    at org.apache.hadoop.hdfs.server.namenode.NameNode$NameNodeHAContext.checkOperation(NameNode.java:2101)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.checkOperation(FSNamesystem.java:1585)
    at org.apache.hadoop.hdfs.server.namenode.NameNodeRpcServer.modifyCacheDirective(NameNodeRpcServer.java:2088)
    ...
```

## Failure Pattern

**8 test failures** across multiple tests with similar patterns:
- Tests that use HA clusters (e.g., `TestCacheDirectives`, `TestPipelinesFailover`, `TestConsistentReadsObserver`, `TestRollingUpgradeDowngrade`)
- Restart positions that target the Active NameNode
- Failures occur during WRITE operations after the restart

## Root Cause Analysis

### Same Root Cause as Groups 2 and 3

Group 14 has the identical root cause as Groups 2 and 3:

1. **Restart Injection**: The restart framework triggers a restart targeting the Active NameNode.

2. **NameNode Restart Behavior**: When `MiniDFSCluster.restartNameNode()` is called, the new NameNode starts in **STANDBY state** by default - this is normal HDFS behavior.

3. **Invalid Cluster State** (before fix): After restart, the cluster has no Active NameNode:
   - **NN[0]**: STANDBY (was Active before restart, came back as Standby)
   - **NN[1]**: STANDBY

   **No NameNode is Active!**

4. **WRITE Operation Failure**: When the test tries to perform a WRITE operation (like `modifyCacheDirective()` in TestCacheDirectives), the operation fails with StandbyException because there's no Active NameNode.

### Difference from Groups 2 and 3

The exception stack trace format slightly differs from Groups 2 and 3:
- **Group 2**: `org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)` at top level
- **Group 3**: `org.apache.hadoop.ipc.StandbyException` at top level
- **Group 14**: `Caused by: org.apache.hadoop.ipc.RemoteException(org.apache.hadoop.ipc.StandbyException)` - wrapped inside another exception

All three groups share the same root cause - HA state not being restored after Active NameNode restart.

### The Fix

The restart framework has been updated to properly restore HA state after restart in `HdfsClusterAdapter.restoreHAState()`:

```java
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

## Evidence from Reproduction

Multiple tests were executed to reproduce the failure. All tests now pass because the restart framework has been fixed:

```bash
# Test 1: TestCacheDirectives
mvn surefire:test -Dtest=org.apache.hadoop.hdfs.server.namenode.TestCacheDirectives_RestartInjected#testExpiryTimeConsistency \
    -Drestart.position=after_add_directive -Drestart.target=namenode -Drestart.mode=GRACEFUL
# Result: PASSED

# Test 2: TestPipelinesFailover
mvn surefire:test -Dtest=org.apache.hadoop.hdfs.server.namenode.ha.TestPipelinesFailover_RestartInjected#testLeaseRecoveryAfterFailover \
    -Drestart.position=after_lease_recovery -Drestart.target=namenode -Drestart.mode=GRACEFUL
# Result: PASSED

# Test 3: TestRollingUpgradeDowngrade
mvn surefire:test -Dtest=org.apache.hadoop.hdfs.TestRollingUpgradeDowngrade_RestartInjected#testDowngrade \
    -Drestart.position=after_active_transition -Drestart.target=namenode -Drestart.mode=GRACEFUL
# Result: PASSED
```

**All tests now pass** because the restart framework has been fixed to restore HA state.

## Affected Tests

| Test Class | Test Method | Restart Position | Target |
|-----------|------------|------------------|--------|
| TestCacheDirectives_RestartInjected | testExpiryTimeConsistency | after_add_directive | namenode:0 |
| TestPipelinesFailover_RestartInjected | testLeaseRecoveryAfterFailover | after_lease_recovery | namenode:1 |
| TestPipelinesFailover_RestartInjected | testLeaseRecoveryAfterFailover | after_failback_lease | namenode:0 |
| TestPipelinesFailover_RestartInjected | testFailoverRightBeforeCommitSynchronization | after_failover_commit_sync | namenode:1 |
| TestPipelinesFailover_RestartInjected | testFailoverRightBeforeCommitSynchronization | after_lease_recovery_commit_sync | namenode:1 |
| TestConsistentReadsObserver_RestartInjected | testAutoMsyncLongPeriod | after_get_filesystem | namenode:0 |
| TestRollingUpgradeDowngrade_RestartInjected | testDowngrade | after_active_transition | namenode:0 |
| TestRollingUpgradeDowngrade_RestartInjected | testDowngrade | after_upgrade_prepare | namenode:0 |

## Conclusion

This is a **FALSE POSITIVE** because:

1. The StandbyException is the correct HDFS behavior when attempting WRITE operations on a non-Active NameNode.
2. The original issue was in the restart framework not restoring HA state after restart.
3. The restart framework has been fixed (`HdfsClusterAdapter.restoreHAState()`) to properly transition the NameNode back to ACTIVE after restart.
4. Tests now pass with the fix, confirming the issue was in the restart framework, not HDFS.

## Related

- **Group 2 (FP-GROUP-2.md)**: Same root cause, RemoteException at top level
- **Group 3 (FP-GROUP-3.md)**: Same root cause, StandbyException directly thrown
- **HdfsClusterAdapter.java:253-293**: HA state restoration fix
- **StandbyState.java:108**: Where StandbyException is thrown
