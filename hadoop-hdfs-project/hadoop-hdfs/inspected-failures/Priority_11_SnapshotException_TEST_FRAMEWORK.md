# Priorities 11-12: SnapshotException - Nested Snapshottable Directories

**Status:** 🔧 TEST FRAMEWORK ISSUE
**Category:** Test Transformation Bug - In-Memory Test Settings Not Re-Applied After Restart
**Severity:** N/A (Not a production bug)

**Note:** Priority 11 and Priority 12 are the **same bug** affecting different test methods in the same test class. They are consolidated into this single report.

---

## Executive Summary

The test fails with `SnapshotException: Nested snapshottable directories not allowed` after NameNode restart. Investigation confirms this is **NOT a production bug**, but a **test transformation issue**. The test uses a test-only method `setAllowNestedSnapshots(true)` to enable nested snapshots before restart, but this in-memory setting is not persisted and needs to be re-applied after restart.

**Key Finding:** `setAllowNestedSnapshots()` is marked **"Used in tests only"** in `SnapshotManager.java:154` and is a non-persistent, in-memory flag that defaults to `false` after NameNode restart.

---

## Reproduction

### Affected Test
```
TestSnapshottableDirListing_RestartInjected.testListSnapshottableDir_AfterAllowDir1_NN_Graceful
```

### Error Message
```
org.apache.hadoop.hdfs.protocol.SnapshotException:
Nested snapshottable directories not allowed: path=/TestSnapshot1/sub1,
the ancestor /TestSnapshot1 is already a snapshottable directory.
```

### Test Output
```bash
mvn surefire:test -Dtest=TestSnapshottableDirListing_RestartInjected#testListSnapshottableDir_AfterAllowDir1_NN_Graceful

[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
[ERROR] TestSnapshottableDirListing_RestartInjected.testListSnapshottableDir_AfterAllowDir1_NN_Graceful:134 » Snapshot
```

---

## Root Cause Analysis

### Timeline of Events

**Original Test (TestSnapshottableDirListing.java):**
```java
// Line 79 - Enable nested snapshots
cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);

// Lines 99-108 - Make dir1 snapshottable
hdfs.allowSnapshot(dir1);

// Lines 139-147 - Create subdirectories and make THEM snapshottable (nested)
Path sub1 = new Path(dir1, "sub1");
Path sub2 = new Path(dir1, "sub2");
DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
hdfs.allowSnapshot(sub1);  // ✅ WORKS - nested snapshots enabled
hdfs.allowSnapshot(sub2);  // ✅ WORKS
```

**Restart-Injected Test (TestSnapshottableDirListing_RestartInjected.java):**
```java
// Line 86 - Enable nested snapshots on OLD NameNode
cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);

// Line 99 - Make dir1 snapshottable
hdfs.allowSnapshot(dir1);

// Line 100 - ===== RESTART OCCURS HERE =====
executeRestart();

// Lines 128-135 - Create subdirectories and try to make them snapshottable
Path sub1 = new Path(dir1, "sub1");
Path sub2 = new Path(dir1, "sub2");
DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
hdfs.allowSnapshot(sub1);  // ❌ FAILS - nested snapshots NOT enabled on new NameNode!
hdfs.allowSnapshot(sub2);
```

### The Bug in Detail

1. **Before Restart (Line 86):**
   `cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);`
   Sets the flag to `true` on the **OLD** NameNode instance

2. **Restart Occurs (Line 100):**
   - Old NameNode shuts down
   - New NameNode instance created
   - New SnapshotManager instantiated with **default value: `allowNestedSnapshots = false`**

3. **After Restart (Line 134):**
   `hdfs.allowSnapshot(sub1);`
   Tries to create nested snapshot, but new NameNode has `allowNestedSnapshots = false`
   → Throws `SnapshotException`

---

## Evidence

### 1. The Setting is Test-Only

From `SnapshotManager.java:154`:
```java
/** Used in tests only */
void setAllowNestedSnapshots(boolean allowNestedSnapshots) {
  this.allowNestedSnapshots = allowNestedSnapshots;
}
```

This is explicitly marked as **test-only** functionality, not a production feature.

### 2. The Setting is Not Persistent

From `SnapshotManager.java:101`:
```java
private boolean allowNestedSnapshots = false;
```

This is a **simple instance variable** with no persistence mechanism:
- Not saved to fsimage
- Not written to editlog
- Not configurable via `hdfs-site.xml`
- Defaults to `false` on every NameNode instantiation

### 3. How the Check Works

From `SnapshotManager.java:162-179`:
```java
private void checkNestedSnapshottable(INodeDirectory dir, String path)
    throws SnapshotException {
  if (allowNestedSnapshots) {
    return;  // Skip check if nested snapshots allowed
  }

  for(INodeDirectory s : snapshottables.values()) {
    if (s.isAncestorDirectory(dir)) {
      throw new SnapshotException(
          "Nested snapshottable directories not allowed: path=" + path
          + ", the subdirectory " + s.getFullPathName()
          + " is already a snapshottable directory.");
    }
    if (dir.isAncestorDirectory(s)) {
      throw new SnapshotException(
          "Nested snapshottable directories not allowed: path=" + path
          + ", the ancestor " + s.getFullPathName()  // THIS IS THE ERROR WE SEE
          + " is already a snapshottable directory.");
    }
  }
}
```

**Before Restart:**
`allowNestedSnapshots = true` → check returns early → nested snapshots allowed

**After Restart:**
`allowNestedSnapshots = false` → check proceeds → detects `/TestSnapshot1` is ancestor of `/TestSnapshot1/sub1` → throws exception

---

## Why This is NOT a Production Bug

### 1. Test-Only Feature
The `setAllowNestedSnapshots()` method is explicitly marked "Used in tests only". Production HDFS does not support nested snapshottable directories.

### 2. No Production API
There is no configuration property, RPC method, or admin command to enable nested snapshots in production. This feature exists solely for testing HDFS snapshot functionality.

### 3. Expected Behavior
After NameNode restart in production:
- All persistent state is restored from fsimage/editlog
- Non-persistent test flags revert to defaults
- This is **correct and expected behavior**

### 4. Transformation Tool Limitation
The restart injection framework correctly restarts the NameNode. The issue is that the test transformation didn't recognize that test-only runtime settings need to be re-applied after restart.

---

## Impact Assessment

**Production Impact:** NONE
- This affects only test code
- Nested snapshots are not a production feature
- No production workloads rely on this functionality

**Test Framework Impact:** HIGH
- All restart-injected tests using `setAllowNestedSnapshots()` will fail
- Affects multiple test methods in `TestSnapshottableDirListing_RestartInjected`
- Creates false-positive bug reports

---

## Fix Required

### Immediate Fix: Update Test Transformation

The restart-injected test needs to **re-apply** the test-only setting after restart:

```java
// Line 86 - Enable nested snapshots
cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);

// Line 99 - Make dir1 snapshottable
hdfs.allowSnapshot(dir1);

// Line 100 - Restart
executeRestart();

// ===== ADD THIS LINE AFTER RESTART =====
cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);

// Lines 128-135 - Now nested snapshots will work
Path sub1 = new Path(dir1, "sub1");
hdfs.allowSnapshot(sub1);  // ✅ Will work now
```

### Long-Term Fix: Improve Transformation Tool

The test transformation framework should:

1. **Detect test-only runtime settings:**
   - Scan for methods marked "Used in tests only"
   - Detect calls to `set*()` methods on test components
   - Flag these as needing re-application after restart

2. **Auto-generate re-application code:**
   ```java
   // Before restart
   cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);

   // Tool generates:
   executeRestart();
   cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);  // Auto-reapplied
   ```

3. **Document transformation limitations:**
   - Add comments warning about non-persistent settings
   - Provide guidance for manual fixes

---

## Affected Tests

All test methods in `TestSnapshottableDirListing_RestartInjected` that use nested snapshots:

### Priority 11 - testListSnapshottableDir methods:
1. `testListSnapshottableDir_AfterAllowDir1_NN_Graceful` (fails at line 134)
2. `testListSnapshottableDir_AfterAllowDir1_NN_Crash`
3. `testListSnapshottableDir_AfterAllowDir1_NNAndDN_Graceful`
4. `testListSnapshottableDir_AfterAllowDir1_NNAndDN_Crash`

**Paths affected:** `/TestSnapshot1/sub1`, `/TestSnapshot1/sub2`

### Priority 12 - testListWithDifferentUser methods:
5. `testListWithDifferentUser_AfterAllowInitial_NN_Graceful` (fails at line 823)
6. `testListWithDifferentUser_AfterAllowInitial_NN_Crash`
7. `testListWithDifferentUser_AfterAllowInitial_NNAndDN_Graceful`
8. `testListWithDifferentUser_AfterAllowInitial_NNAndDN_Crash`

**Paths affected:** `/dir_user2/subdir`

**Total:** 8 test methods (4 from Priority 11 + 4 from Priority 12)

**Note:** These tests all call `setAllowNestedSnapshots(true)` before restart but need to re-call it after restart.

---

## Verification

### Test Without Fix (Current Behavior)

**Priority 11 test:**
```bash
mvn surefire:test -Dtest=TestSnapshottableDirListing_RestartInjected#testListSnapshottableDir_AfterAllowDir1_NN_Graceful

# Result: FAILS with SnapshotException at line 134
# Error: "path=/TestSnapshot1/sub1, the ancestor /TestSnapshot1 is already a snapshottable directory"
```

**Priority 12 test:**
```bash
mvn surefire:test -Dtest=TestSnapshottableDirListing_RestartInjected#testListWithDifferentUser_AfterAllowInitial_NN_Graceful

# Result: FAILS with SnapshotException at line 823
# Error: "path=/dir_user2/subdir, the ancestor /dir_user2 is already a snapshottable directory"
```

### Expected Behavior After Fix
```bash
# Both tests should pass after re-applying setAllowNestedSnapshots(true) after restart
mvn surefire:test -Dtest=TestSnapshottableDirListing_RestartInjected#testListSnapshottableDir_AfterAllowDir1_NN_Graceful
mvn surefire:test -Dtest=TestSnapshottableDirListing_RestartInjected#testListWithDifferentUser_AfterAllowInitial_NN_Graceful

# Result: PASSES ✅
```

---

## Comparison with Similar Issues

This is the **exact same pattern** as:
- **Priority 2:** Stale `BlockManager` references after restart
- **Priority 3:** Stale `FSNamesystem` references after restart

**Common Pattern:**
1. Test obtains reference or sets state on OLD component before restart
2. Restart creates NEW component instances
3. Test continues using OLD references or assumes OLD state persists
4. Test fails because NEW components don't have the same state

**Root Cause:**
All three issues stem from the test transformation tool not understanding which test state needs to be refreshed or re-applied after restart.

---

## References

### Test Files
- **Original:** `TestSnapshottableDirListing.java:79` - Sets nested snapshots, no restart, works
- **Restart-Injected:** `TestSnapshottableDirListing_RestartInjected.java:86,134` - Sets before restart, fails after

### Production Code
- **SnapshotManager.java:101** - Field declaration: `private boolean allowNestedSnapshots = false;`
- **SnapshotManager.java:154** - Setter marked "Used in tests only"
- **SnapshotManager.java:162-179** - Check that throws the exception

### Error Location
- **Stack trace:** `SnapshotManager.checkNestedSnapshottable():178`
- **Test failure:** `TestSnapshottableDirListing_RestartInjected.java:134`

---

## Conclusion

This is a **confirmed test framework issue**, not a production bug. The `setAllowNestedSnapshots()` method is:
- Marked as test-only
- Non-persistent in-memory flag
- Not designed to survive restarts

**Fix:** Re-apply `setAllowNestedSnapshots(true)` after restart in the test.

**Recommendation:** Update test transformation tool to automatically detect and re-apply test-only runtime settings after restart injections.

---

*Report Date: 2025-12-01*
*Investigation Method: Code comparison + source analysis*
*Root Cause: Test transformation tool limitation*
*Production Impact: NONE*
