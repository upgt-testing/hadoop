# HDFS-BUG-GROUP-26: Stale FSNamesystem Reference After Namenode Restart

## Classification
**TEST-BUG**

## Summary
The restart-injected test `TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage` fails with NPE because the test stores a reference to `FSNamesystem` (`fsn` field) in the `@Before` setup, but does not refresh this reference after a namenode restart. After restart, the test continues to use the stale reference to the old (closed) FSNamesystem, leading to inconsistent behavior and ultimately an NPE when loading the saved FSImage.

## Failure Details

### Stacktrace
```
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.server.namenode.snapshot.FSImageFormatPBSnapshot$Loader.loadSnapshotSection(FSImageFormatPBSnapshot.java:154)
    at org.apache.hadoop.hdfs.server.namenode.FSImageFormatProtobuf$Loader.loadInternal(FSImageFormatProtobuf.java:464)
    at org.apache.hadoop.hdfs.server.namenode.FSImageFormatProtobuf$Loader.load(FSImageFormatProtobuf.java:247)
    at org.apache.hadoop.hdfs.server.namenode.FSImageFormat$LoaderDelegator.load(FSImageFormat.java:228)
    at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.loadFSImageFromTempFile(TestFSImageWithSnapshot_RestartInjected.java:166)
    at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.checkImage(TestFSImageWithSnapshot_RestartInjected.java:424)
    at org.apache.hadoop.hdfs.server.namenode.TestFSImageWithSnapshot_RestartInjected.testSaveLoadImage(TestFSImageWithSnapshot_RestartInjected.java:282)
```

### Restart Configuration
- Position: `after_snapshot_s1`
- Target: `namenode`
- Mode: `GRACEFUL`

## Root Cause Analysis

### Debug Evidence
Added debug logging to verify the stale reference:
```java
System.out.println("DEBUG: fsn reference: " + System.identityHashCode(fsn));
System.out.println("DEBUG: cluster.getNamesystem() reference: " + System.identityHashCode(cluster.getNamesystem()));
System.out.println("DEBUG: fsn == cluster.getNamesystem(): " + (fsn == cluster.getNamesystem()));
```

Output:
```
DEBUG: fsn reference: 105751207
DEBUG: cluster.getNamesystem() reference: 1171713907
DEBUG: fsn == cluster.getNamesystem(): false
```

This proves that the test's `fsn` field (105751207) is different from the current `cluster.getNamesystem()` (1171713907) after the namenode restart.

### Detailed Flow

1. **Test Setup (`@Before`)**:
   ```java
   fsn = cluster.getNamesystem();  // fsn = FSNamesystem#1
   ```

2. **Test creates snapshot "s1"**:
   ```java
   SnapshotTestHelper.createSnapshot(hdfs, dir, "s" + ++s);
   ```

3. **Namenode restart is triggered at `after_snapshot_s1`**:
   ```java
   RestartFramework.at("after_snapshot_s1")
       .on(cluster)
       .restart("namenode")
       .withIndex(0)
       .withMode(RestartMode.GRACEFUL)
       .execute();
   ```
   - After restart, the cluster's namenode has a NEW FSNamesystem (FSNamesystem#2)
   - But the test's `fsn` field STILL points to FSNamesystem#1 (which has been closed!)

4. **Test calls `checkImage()`**:
   - `dumpTree2File()` uses `fsn.getFSDirectory()` - accesses the OLD closed FSNamesystem
   - `saveFSImageToTempFile()` uses `fsn` - saves state from the OLD closed FSNamesystem
   - The saved FSImage contains inconsistent/corrupted data from the closed FSNamesystem

5. **FSImage loading fails**:
   - When loading the corrupted FSImage into the fresh cluster, `loadSnapshotSection()` tries to find INodes by ID
   - The INode IDs in the snapshot section don't match the INodes loaded (because the FSImage data is inconsistent)
   - `fsDir.getInode(sdirId)` returns `null`, causing the NPE

### Buggy Code Location

**File**: `TestFSImageWithSnapshot_RestartInjected.java`

**Setup storing the reference**:
```java
@Before
public void setUp() throws Exception {
    conf = new Configuration();
    cluster = new MiniDFSCluster.Builder(conf).numDataNodes(NUM_DATANODES)
        .build();
    cluster.waitActive();
    fsn = cluster.getNamesystem();  // <-- This reference becomes stale after restart
    hdfs = cluster.getFileSystem();
}
```

**Test method missing fsn refresh after restart**:
```java
@Test
public void testSaveLoadImage() throws Exception {
    int s = 0;
    hdfs.mkdirs(dir);
    SnapshotTestHelper.createSnapshot(hdfs, dir, "s" + ++s);

    RestartFramework.at("after_snapshot_s1")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    // MISSING: fsn = cluster.getNamesystem();

    Path sub1 = new Path(dir, "sub1");
    ...
    checkImage(s);  // Uses stale fsn reference
```

**Methods using stale fsn**:
```java
private File saveFSImageToTempFile() throws IOException {
    SaveNamespaceContext context = new SaveNamespaceContext(fsn, txid,  // stale fsn
        new Canceler());
    ...
}

private File dumpTree2File(String fileSuffix) throws IOException {
    File file = getDumpTreeFile(testDir, fileSuffix);
    SnapshotTestHelper.dumpTree2File(fsn.getFSDirectory(), file);  // stale fsn
    return file;
}
```

## Potential Fix

After each namenode restart, refresh the `fsn` reference:

```java
RestartFramework.at("after_snapshot_s1")
    .on(cluster)
    .restart("namenode")
    .withIndex(0)
    .withMode(RestartMode.GRACEFUL)
    .execute();

// Fix: Refresh fsn reference after namenode restart
fsn = cluster.getNamesystem();
```

The fix should be applied after EVERY namenode restart in the test:
- Line 262-267: after `after_snapshot_s1`
- Line 275-280: after `after_mkdir_sub11`
- Line 286-291: after `after_snapshot_s2`
- Line 309-314: after `after_snapshot_s3`
- Line 333-338: after `after_snapshot_s4`
- Line 345-350: after `after_modifications`
- Line 356-361: after `after_snapshot_s5`
- Line 366-371: after `after_first_rename`
- Line 375-380: after `after_second_rename`
- Line 385-390: after `after_third_rename`

## Related Groups
This is the same pattern as:
- **Group 4**: Stale namenode references causing NPE in BlocksMap.getStoredBlock
- **Group 19**: Stale `nn` reference causing NPE in BlocksMap.numNodes

All these groups share the same root cause: restart-injected tests need to refresh references to namenode components after any namenode restart.

## Defensive Fix in Production Code

A defensive fix was applied to `FSImageFormatPBSnapshot.java` to provide better error messages instead of cryptic NPEs. This helps diagnose the issue more quickly when similar problems occur.

### Before (NPE with no context):
```
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.server.namenode.snapshot.FSImageFormatPBSnapshot$Loader.loadSnapshotSection(FSImageFormatPBSnapshot.java:154)
```

### After (Informative IOException):
```
java.io.IOException: Failed to load snapshottable directory: INode with id=16386 not found in FSDirectory.
This may indicate a corrupted FSImage where the snapshot section references an INode that was not loaded
in the INode section, or the FSImage was saved from a stale/closed FSNamesystem.
Expected snapshottable directories: [16386], numSnapshots=1
```

### Code Changes

**File**: `hadoop-hdfs/src/main/java/org/apache/hadoop/hdfs/server/namenode/snapshot/FSImageFormatPBSnapshot.java`

**1. `loadSnapshotSection()` method** - Added null check and type validation:
```java
for (long sdirId : section.getSnapshottableDirList()) {
    INode inode = fsDir.getInode(sdirId);
    if (inode == null) {
        throw new IOException("Failed to load snapshottable directory: " +
            "INode with id=" + sdirId + " not found in FSDirectory. " +
            "This may indicate a corrupted FSImage where the snapshot section " +
            "references an INode that was not loaded in the INode section, " +
            "or the FSImage was saved from a stale/closed FSNamesystem. " +
            "Expected snapshottable directories: " + section.getSnapshottableDirList() +
            ", numSnapshots=" + snum);
    }
    if (!inode.isDirectory()) {
        throw new IOException("Failed to load snapshottable directory: " +
            "INode with id=" + sdirId + " is not a directory (type=" +
            inode.getClass().getSimpleName() + "). FSImage may be corrupted.");
    }
    INodeDirectory dir = inode.asDirectory();
    // ... rest of the method
}
```

**2. `loadSnapshots()` method** - Added null check and type validation for parent directory.

**3. `loadSnapshotDiffSection()` method** - Added null check and type validation for diff entries.

---

## Pull Request Information

### Patch File
```
dec20/patches/HDFS-XXXXX-improve-fsimage-snapshot-loading-error-messages.patch
```

### How to Apply
```bash
# From the HDFS repository root:
git apply dec20/patches/HDFS-XXXXX-improve-fsimage-snapshot-loading-error-messages.patch
```

### Suggested JIRA Title
```
HDFS-XXXXX. Improve error messages when loading snapshot sections from FSImage
```

### Suggested PR Description

**Summary**

When loading an FSImage with corrupted or inconsistent snapshot data, the NameNode currently throws cryptic NullPointerExceptions that provide no diagnostic information. This patch adds defensive null checks and type validation to provide helpful error messages.

**Problem**

During FSImage loading, if the snapshot section references an INode ID that doesn't exist in the INode section, the code throws an unhelpful NPE:
```
java.lang.NullPointerException
    at FSImageFormatPBSnapshot$Loader.loadSnapshotSection(FSImageFormatPBSnapshot.java:154)
```

This can happen when:
- The FSImage file is corrupted
- The FSImage was saved from a stale/closed FSNamesystem (e.g., during testing)
- There's a bug in the FSImage saving logic

**Solution**

Add defensive null checks and type validation in three methods of `FSImageFormatPBSnapshot.java`:
1. `loadSnapshotSection()` - validates snapshottable directory INodes
2. `loadSnapshots()` - validates parent directory INodes for snapshots
3. `loadSnapshotDiffSection()` - validates INodes referenced in diff entries

**After this change**, users get helpful error messages like:
```
java.io.IOException: Failed to load snapshottable directory: INode with id=16386
not found in FSDirectory. This may indicate a corrupted FSImage where the snapshot
section references an INode that was not loaded in the INode section, or the FSImage
was saved from a stale/closed FSNamesystem. Expected snapshottable directories: [16386],
numSnapshots=1
```

**Testing**

- Verified the patch compiles successfully
- Verified the improved error message is produced when loading corrupted FSImage data

**Impact**

This is a low-risk change that only adds defensive error checking. It does not change normal behavior - it only improves error messages when something goes wrong.

| Aspect | Details |
|--------|---------|
| File Modified | `FSImageFormatPBSnapshot.java` |
| Lines Changed | +66, -5 |
| Risk Level | Low (defensive checks only) |
| Backward Compatible | Yes |
