# FP-GROUP-7: SnapshotException - Nested Snapshottable Directories Not Allowed

## Summary
**Classification**: FALSE POSITIVE

The failure occurs because the test relies on an in-memory only setting `allowNestedSnapshots` that does not persist across NameNode restarts. After restart, the NameNode reverts to the default behavior of disallowing nested snapshottable directories.

## Failure Details

**Test**: `TestSnapshotDiffReport_RestartInjected#testDiffReport`

**Restart Position**: `after_mkdir_subsubsub1`

**Target**: `namenode` (index 0)

**Mode**: `GRACEFUL`

**Error**:
```
org.apache.hadoop.hdfs.protocol.SnapshotException:
Nested snapshottable directories not allowed: path=/TestSnapshot/sub1/subsub1/subsubsub1,
the ancestor /TestSnapshot/sub1 is already a snapshottable directory.
    at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.checkNestedSnapshottable(SnapshotManager.java:178)
    at org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotManager.setSnapshottable(SnapshotManager.java:193)
    at org.apache.hadoop.hdfs.server.namenode.FSDirSnapshotOp.allowSnapshot(FSDirSnapshotOp.java:63)
    at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.allowSnapshot(FSNamesystem.java:6931)
```

## Root Cause Analysis

### Test Setup
The test `testDiffReport` is designed to test nested snapshot functionality:

```java
// Line 207 - Enable nested snapshots (IN-MEMORY ONLY)
cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);

Path subsub1 = new Path(sub1, "subsub1");
Path subsubsub1 = new Path(subsub1, "subsubsub1");
hdfs.mkdirs(subsubsub1);  // Creates /TestSnapshot/sub1/subsub1/subsubsub1

// Line 213-218 - RESTART IS INJECTED HERE
RestartFramework.at("after_mkdir_subsubsub1")
    .on(cluster)
    .restart("namenode")
    ...

// Line 220 - After restart, this fails
modifyAndCreateSnapshot(sub1, new Path[]{sub1, subsubsub1});
```

### The Problem

1. **In-Memory Setting**: The `allowNestedSnapshots` flag in `SnapshotManager` is a pure in-memory boolean field with default value `false`:
   ```java
   // SnapshotManager.java:101
   private boolean allowNestedSnapshots = false;
   ```

2. **Not Persisted**: This setting is NOT:
   - Saved to FSImage or edit logs
   - Configured via `hdfs-site.xml` or any configuration key
   - Restored after NameNode restart

3. **After Restart**: When the NameNode restarts, the `SnapshotManager` is recreated with `allowNestedSnapshots = false` (default).

4. **Nested Check Fails**: When `modifyAndCreateSnapshot` calls `hdfs.allowSnapshot(subsubsub1)`:
   ```java
   // SnapshotManager.java:162-178
   private void checkNestedSnapshottable(INodeDirectory dir, String path)
       throws SnapshotException {
     if (allowNestedSnapshots) {  // Now FALSE after restart!
       return;
     }
     // Check for nesting and throw exception
     for(INodeDirectory s : snapshottables.values()) {
       if (s.isAncestorDirectory(dir)) {
         throw new SnapshotException("Nested snapshottable directories not allowed...");
       }
       ...
     }
   }
   ```

## Why This Is a False Positive

1. **Test-Only Configuration**: The `setAllowNestedSnapshots(true)` method is explicitly designed for testing nested snapshot features. There is no corresponding configuration key (checked in `DFSConfigKeys.java`) for production use.

2. **Design Intent**: HDFS deliberately disallows nested snapshottable directories by default because:
   - It can lead to confusing snapshot semantics
   - Storage and computation overhead increases
   - Managing nested snapshots is complex

3. **Framework Limitation**: The restart framework cannot preserve arbitrary in-memory state like `allowNestedSnapshots`. This is not a flaw - it's correct behavior to restart with clean defaults.

4. **Improper Restart Position**: Injecting a restart at `after_mkdir_subsubsub1` for a test that relies on special in-memory configuration creates an invalid test scenario. The test was designed to run without restarts while nested snapshots are enabled.

## Affected Test Executions (19 total)

All 19 failures in Group 7 involve tests that call `setAllowNestedSnapshots(true)` followed by snapshot operations that create nested snapshottable directories. The restart positions occur after the in-memory setting but before the snapshot operations complete.

## Recommendation

Mark these restart positions as incompatible with tests that use `setAllowNestedSnapshots(true)`. The restart framework should either:
1. Skip restart injection for tests that modify SnapshotManager's in-memory state, OR
2. Add logic to restore the `allowNestedSnapshots` state after restart (though this would be artificial and not reflect real-world behavior)

Option 1 is preferred as it maintains realistic restart semantics.
