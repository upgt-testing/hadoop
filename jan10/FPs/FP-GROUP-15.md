# FP-GROUP-15: IllegalMonitorStateException in ReentrantReadWriteLock

## Classification: FALSE POSITIVE

## Summary

This failure is caused by an **improper restart position** that injects a restart in the middle of a critical section protected by a write lock. After restart, the test code reassigns its FSNamesystem reference to the new instance and attempts to unlock it, but the new instance was never locked by the current thread.

## Failure Details

- **Exception**: `java.lang.IllegalMonitorStateException`
- **Location**: `ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java:371)`
- **Test Executions**: 7 failures
- **Restart Position**: `after_file_deletion`
- **Restart Target**: `namenode`
- **Restart Mode**: `GRACEFUL`

## Stack Trace

```
java.lang.IllegalMonitorStateException
    at java.util.concurrent.locks.ReentrantReadWriteLock$Sync.tryRelease(ReentrantReadWriteLock.java:371)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.release(AbstractQueuedSynchronizer.java:1261)
    at java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock.unlock(ReentrantReadWriteLock.java:1131)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystemLock.writeUnlock(FSNamesystemLock.java:284)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystemLock.writeUnlock(FSNamesystemLock.java:236)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.writeUnlock(FSNamesystem.java:1765)
    at org.apache.hadoop.hdfs.server.namenode.TestListOpenFiles_RestartInjected.testListOpenFilesWithDeletedPath(TestListOpenFiles_RestartInjected.java:464)
```

## Root Cause Analysis

### Original Test Code (TestListOpenFiles.java)

```java
@Test
public void testListOpenFilesWithDeletedPath() throws Exception {
    // ... setup code ...
    FSNamesystem fsNamesystem = cluster.getNamesystem();  // Line 337
    // ...
    fsNamesystem.writeLock();  // Line 341: Acquire lock
    try {
        dir.removeFromInodeMap(removedINodes);  // Line 343
        // ... verify operations ...
    } finally {
        fsNamesystem.writeUnlock();  // Line 352: Release lock on SAME object
    }
}
```

### Restart-Injected Test Code (TestListOpenFiles_RestartInjected.java)

```java
@Test
public void testListOpenFilesWithDeletedPath() throws Exception {
    // ... setup code ...
    FSNamesystem fsNamesystem = cluster.getNamesystem();  // Line 441: OLD FSNamesystem
    // ...
    fsNamesystem.writeLock();  // Line 445: Acquire lock on OLD FSNamesystem
    try {
        dir.removeFromInodeMap(removedINodes);  // Line 447

        // RESTART INJECTED HERE (Lines 448-453)
        RestartFramework.at("after_file_deletion")
            .on(cluster)
            .restart("namenode")
            .withIndex(0)
            .withMode(RestartMode.GRACEFUL)
            .execute();

        fsNamesystem = cluster.getNamesystem();  // Line 454: NEW FSNamesystem!
        dir = cluster.getNamesystem().getFSDirectory();  // Line 455
        // ... verify operations ...
    } finally {
        fsNamesystem.writeUnlock();  // Line 464: Try to unlock NEW FSNamesystem - FAILS!
    }
}
```

### The Problem Sequence

1. **Line 445**: `fsNamesystem.writeLock()` acquires write lock on the **OLD** FSNamesystem instance
2. **Lines 448-453**: Restart is executed, which:
   - Shuts down the old namenode (along with the OLD FSNamesystem)
   - Starts a new namenode (with a NEW FSNamesystem)
3. **Line 454**: `fsNamesystem = cluster.getNamesystem()` reassigns the variable to the **NEW** FSNamesystem
4. **Line 464**: `fsNamesystem.writeUnlock()` attempts to release the lock on the **NEW** FSNamesystem

The IllegalMonitorStateException occurs because:
- The current thread holds the write lock on the OLD FSNamesystem (which is now destroyed)
- The current thread does NOT hold any lock on the NEW FSNamesystem
- Attempting to unlock a lock you don't hold throws IllegalMonitorStateException

## Why This is a False Positive

1. **Improper Restart Position**: The restart is injected inside a critical section (between `writeLock()` and `writeUnlock()`). This is an improper restart position because:
   - It invalidates the object that holds the lock
   - It breaks the lock/unlock pairing that is fundamental to proper lock usage

2. **Test Code Pattern Issue**: The test code pattern of reassigning the `fsNamesystem` variable after restart breaks the contract that `unlock()` must be called on the same object as `lock()`.

3. **Not a Source Code Bug**: The HDFS source code (`FSNamesystemLock`, `FSNamesystem`) is functioning correctly. The lock implementation correctly throws `IllegalMonitorStateException` when a thread tries to release a lock it doesn't hold.

## Affected Test Executions

| Test Class | Test Method | Position | Target | Mode |
|------------|-------------|----------|--------|------|
| TestListOpenFiles_RestartInjected | testListOpenFilesWithDeletedPath | after_file_deletion | namenode | GRACEFUL |
| TestPendingReconstruction_RestartInjected | testReplicationCounter | after_blocks_setup | namenode | GRACEFUL |
| TestPendingReconstruction_RestartInjected | testReplicationCounter | after_pending_blocks_added | namenode | GRACEFUL |
| TestPendingReconstruction_RestartInjected | testReplicationCounter | after_successful_replication | namenode | GRACEFUL |
| TestPendingReconstruction_RestartInjected | testReplicationCounter | after_failed_replication | namenode | GRACEFUL |
| TestINodeFileUnderConstructionWithSnapshot_RestartInjected | testLease | after_lease_checks | namenode | GRACEFUL |
| TestOverReplicatedBlocks_RestartInjected | testProcesOverReplicateBlock | after_set_replication | namenode | GRACEFUL |

## Conclusion

This is a **FALSE POSITIVE** caused by:
1. Injecting restart inside a critical section protected by a write lock
2. Test code reassigning the FSNamesystem reference after restart, breaking the lock/unlock pairing

The restart framework should avoid injecting restarts inside synchronized blocks or critical sections. The test code should either:
- Not reassign the FSNamesystem reference, or
- Handle the unlock in a restart-aware manner (e.g., check if restart occurred and skip unlock)
