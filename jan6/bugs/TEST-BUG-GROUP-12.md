# TEST-BUG-GROUP-12: NullPointerException in BlocksMap.getStoredBlock

## Summary

**Classification**: TEST-BUG

**Root Cause**: The restart-injected test `TestSnapshotDeletion_RestartInjected` does not refresh its references to `fsn`, `fsdir`, and `blockmanager` after a NameNode restart. When a NameNode restarts, a new `FSNamesystem`, `FSDirectory`, and `BlockManager` are created, but the test continues using stale references pointing to the old, now-shutdown instances.

## Failure Details

**Test Class**: `org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected`

**Test Method**: `testDeleteEarliestSnapshot2`

**Restart Position**: `after_snapshot_s0_deletion`

**Restart Target**: `namenode` (index 0)

**Restart Mode**: `GRACEFUL`

## Stack Trace

```
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.server.blockmanagement.BlocksMap.getStoredBlock(BlocksMap.java:146)
    at org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.getStoredBlock(BlockManager.java:4625)
    at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap.assertBlockCollection(TestSnapshotBlocksMap.java:103)
    at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotBlocksMap.assertBlockCollection(TestSnapshotBlocksMap.java:96)
    at org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotDeletion_RestartInjected.testDeleteEarliestSnapshot2(TestSnapshotDeletion_RestartInjected.java:715)
```

## Technical Analysis

### 1. Test Setup (Lines 99-110)

In the `@Before` method, the test initializes class-level fields:

```java
@Before
public void setUp() throws Exception {
    conf = new Configuration();
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(REPLICATION)
        .format(true).build();
    cluster.waitActive();

    fsn = cluster.getNamesystem();           // <-- Captured reference
    fsdir = fsn.getFSDirectory();            // <-- Captured reference
    blockmanager = fsn.getBlockManager();    // <-- Captured reference
    hdfs = cluster.getFileSystem();
}
```

### 2. Restart Injection (Lines 686-691)

The restart is injected after `hdfs.deleteSnapshot(dir, "s0")`:

```java
hdfs.deleteSnapshot(dir, "s0");
// ...

RestartFramework.at("after_snapshot_s0_deletion")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();
```

### 3. Stale Reference Usage (Line 715-716)

After the restart, the test continues using the old `blockmanager` and `fsdir` references:

```java
TestSnapshotBlocksMap.assertBlockCollection(noChangeFile.toString(), 1,
    fsdir, blockmanager);  // <-- Uses stale references!
```

### 4. Why NPE Occurs

When the NameNode shuts down during restart, `BlocksMap.close()` is called:

```java
// BlocksMap.java:94-97
void close() {
    clear();
    blocks = null;  // <-- blocks field is set to null
}
```

When `getStoredBlock()` is called on the stale `blockmanager`:
- `BlockManager.getStoredBlock()` calls `blocksMap.getStoredBlock(block)` (line 4625)
- `BlocksMap.getStoredBlock()` executes `return blocks.get(b)` (line 146)
- Since `blocks` is now `null`, an NPE is thrown

## The Fix

The test code should refresh its references after any NameNode restart:

```java
RestartFramework.at("after_snapshot_s0_deletion")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// ADD: Refresh references after NameNode restart
fsn = cluster.getNamesystem();
fsdir = fsn.getFSDirectory();
blockmanager = fsn.getBlockManager();
```

This pattern should be applied after every NameNode restart injection in the test file.

## Affected Tests (All 13 failures in Group 12)

All 13 test executions in this group share the same root cause - the test code uses stale references after NameNode restart. The affected tests include:

1. `TestSnapshotDeletion_RestartInjected.testDeleteEarliestSnapshot2`
2. `TestSnapshotDeletion_RestartInjected.testDeleteCurrentFileDirectory`
3. `TestSnapshotDeletion_RestartInjected.testCombineSnapshotDiff1`
4. `TestSnapshotDeletion_RestartInjected.testCombineSnapshotDiff2`
5. `TestUpdateBlockTailing_RestartInjected.testStandbyAddBlockIBRRace`
6. `TestSnapshotBlocksMap_RestartInjected.testDeletionWithSnapshots`

## Comparison with Original Test

The original `TestSnapshotDeletion.testDeleteEarliestSnapshot2` (lines 479-570) does NOT have this issue because:
- It does not have a restart injection in the middle of the test
- The only restart (`cluster.restartNameNodes()`) in the test class happens at the **end** of test methods, after which no stale references are used

## Why Commit 277f50fd8ad2 ("refresh reference after restart") Doesn't Fix This

Commit `277f50fd8ad2cb9602e2657a64b6f0965b0b5729` attempted to fix stale reference issues but **did not fix Group 12 failures** because:

### 1. Incomplete Scope

| Test File | NameNode Restarts | Reference Refreshes Added | Gap |
|-----------|-------------------|---------------------------|-----|
| TestSnapshotDeletion_RestartInjected | 75 | 3 (only in testHANNRestartAfterSnapshotDeletion) | 72 |
| TestSnapshotBlocksMap_RestartInjected | 24 | 0 | 24 |

### 2. Wrong Variables Refreshed

The commit only refreshed HA-specific local variables:
```java
// What the commit added (testHANNRestartAfterSnapshotDeletion only):
snn = cluster.getNameNode(1);  // standby NameNode reference
ann = cluster.getNameNode(0);  // active NameNode reference
```

But it did NOT refresh the class-level fields used by most tests:
```java
// What was needed but NOT added:
fsn = cluster.getNamesystem();
fsdir = fsn.getFSDirectory();
blockmanager = fsn.getBlockManager();
```

### 3. Single Method vs. Class-Wide Issue

- The commit only fixed `testHANNRestartAfterSnapshotDeletion` method
- The failing test `testDeleteEarliestSnapshot2` was NOT modified
- Other affected tests (`testDeleteCurrentFileDirectory`, `testCombineSnapshotDiff1`, etc.) were also NOT modified

### 4. Verification

```bash
# Count of namenode restarts in TestSnapshotDeletion_RestartInjected.java
$ grep -c 'restart("namenode")' TestSnapshotDeletion_RestartInjected.java
75

# Count of fsn refreshes (excluding setUp)
$ grep -c 'fsn = cluster.getNamesystem()' TestSnapshotDeletion_RestartInjected.java
1  # Only in setUp(), none after restarts
```

## Conclusion

This is a **TEST-BUG** introduced during the restart injection transformation. The HDFS source code is correct. The test code needs to be fixed to refresh its references after NameNode restarts.

The commit 277f50fd8ad2 was a partial fix that only addressed HA-specific variables in one method, leaving the majority of stale reference issues unfixed.
