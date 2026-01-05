# HDFS-BUG-GROUP-58: Stale FSDirectory Reference After NameNode Restart

## Summary

Test `TestCommitBlockWithInvalidGenStamp_RestartInjected.testCommitWithInvalidGenStamp` fails with NullPointerException at `DFSTestUtil.addBlockToFile()` because the test uses a stale `FSDirectory` reference (`dir`) from the old NameNode after a restart.

## Classification

**TEST-BUG**

## Affected Test

- `org.apache.hadoop.hdfs.server.namenode.TestCommitBlockWithInvalidGenStamp_RestartInjected.testCommitWithInvalidGenStamp`
- Restart Position: `after_file_create`
- Restart Target: `namenode`
- Restart Mode: `GRACEFUL`

## Stack Trace

```
java.lang.NullPointerException
    at org.apache.hadoop.hdfs.DFSTestUtil.addBlockToFile(DFSTestUtil.java:2231)
    at org.apache.hadoop.hdfs.server.namenode.TestCommitBlockWithInvalidGenStamp_RestartInjected.testCommitWithInvalidGenStamp(TestCommitBlockWithInvalidGenStamp_RestartInjected.java:89)
```

## Root Cause Analysis

### Problem

1. The test stores `dir = cluster.getNamesystem().getFSDirectory()` in `@Before` setup at line 53
2. After namenode restart at `after_file_create` (lines 72-77), a NEW FSNamesystem with a NEW FSDirectory is created
3. The test's `dir` field still points to the OLD (closed) FSDirectory
4. At line 79 (now 85 with debug code), the test calls `dir.getINode4Write(file.toString())` using the stale `dir`
5. The `fileNode` returned from the OLD FSDirectory has an empty blocks array (`getBlocks() = []`)
6. When `DFSTestUtil.addBlockToFile()` is called:
   - Line 2222-2223: `fs.getClient().namenode.addBlock()` operates on the NEW namenode (correct)
   - Line 2225: `fileNode.getLastBlock()` is called on the stale `fileNode` which returns `null` (because blocks array is empty)
   - Line 2231: `lastBlock.getBlockId()` causes NPE because `lastBlock` is null

### Debug Evidence

Debug output confirms the issue:
```
DEBUG: dir = org.apache.hadoop.hdfs.server.namenode.FSDirectory@889d9e8
DEBUG: dir hashCode = 143251944
DEBUG: NEW dir from cluster = org.apache.hadoop.hdfs.server.namenode.FSDirectory@5246a3b3
DEBUG: NEW dir hashCode = 1380361139
DEBUG: dir == NEW dir? false
DEBUG: fileNode = file
DEBUG: fileNode.getBlocks() = []
DEBUG: fileNode.getLastBlock() = null
```

This shows:
- `dir` (from @Before) is a DIFFERENT object than `cluster.getNamesystem().getFSDirectory()` after restart
- The stale `fileNode` has empty blocks array
- `getLastBlock()` returns null, causing the NPE

## Buggy Code

### TestCommitBlockWithInvalidGenStamp_RestartInjected.java

```java
@Before
public void setUp() throws IOException {
    // ...
    dir = cluster.getNamesystem().getFSDirectory();  // Line 53 - stores reference before restart
    // ...
}

@Test
public void testCommitWithInvalidGenStamp() throws Exception {
    // ...
    out = dfs.create(file, (short) 1);
    RestartFramework.at("after_file_create")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();
    // After restart, 'dir' still points to OLD FSDirectory

    INodeFile fileNode = dir.getINode4Write(file.toString()).asFile();  // Line 79 - uses stale 'dir'
    // fileNode is from OLD FSDirectory, has empty blocks array

    // ...
    Block newBlock = DFSTestUtil.addBlockToFile(false, cluster.getDataNodes(),
        dfs, cluster.getNamesystem(), file.toString(), fileNode,  // fileNode is stale
        dfs.getClient().getClientName(), previous, 0, 100);  // Line 89 - NPE inside
```

### DFSTestUtil.java (where NPE occurs)

```java
public static Block addBlockToFile(boolean isStripedBlock,
    List<DataNode> dataNodes, DistributedFileSystem fs, FSNamesystem ns,
    String file, INodeFile fileNode,  // stale fileNode passed in
    String clientName, ExtendedBlock previous, int numStripes, int len)
    throws Exception {
  fs.getClient().namenode.addBlock(file, clientName, previous, null,
      fileNode.getId(), null, null);  // operates on NEW namenode

  final BlockInfo lastBlock = fileNode.getLastBlock();  // Line 2225 - returns null for stale fileNode
  // ...
  for (int i = 0; i < groupSize; i++) {
    DataNode dn = dataNodes.get(i);
    final Block block = new Block(lastBlock.getBlockId() + i, 0,  // Line 2231 - NPE here!
        lastBlock.getGenerationStamp());
```

## Potential Fix

The restart-injected test should re-fetch `dir` and `fileNode` after any namenode restart:

```java
@Test
public void testCommitWithInvalidGenStamp() throws Exception {
    // ...
    out = dfs.create(file, (short) 1);
    RestartFramework.at("after_file_create")
        .on(cluster)
        .restart("namenode")
        .withIndex(0)
        .withMode(RestartMode.GRACEFUL)
        .execute();

    // Re-fetch dir after restart
    dir = cluster.getNamesystem().getFSDirectory();

    INodeFile fileNode = dir.getINode4Write(file.toString()).asFile();
    // ...
```

## Impact

This is a test-level bug in the restart-injected test. The original test `TestCommitBlockWithInvalidGenStamp` does not have this issue because it doesn't involve namenode restarts. The restart-injected version needs to handle component reference updates after restarts.

## Test Utility Code Improvement

While the root cause is a TEST-BUG (stale reference), the test utility code `DFSTestUtil.addBlockToFile()` can be improved to provide better error messages when this situation occurs. A defensive null check has been added to throw an informative `IllegalStateException` instead of a cryptic `NullPointerException`.

**Patch:** See [HDFS-XXXXX-improve-dfstestutil-addblocktofile-error-messages.patch](../patches/HDFS-XXXXX-improve-dfstestutil-addblocktofile-error-messages.patch)

The patch adds validation after `namenode.addBlock()` to check if `fileNode.getLastBlock()` returns null:

```java
final BlockInfo lastBlock = fileNode.getLastBlock();
if (lastBlock == null) {
  throw new IllegalStateException(
      "fileNode.getLastBlock() returned null after addBlock() call for file '" +
      file + "'. This typically indicates the fileNode is a stale reference " +
      "from a previous NameNode instance that was not updated by the addBlock() " +
      "operation. Current blocks in fileNode: " +
      java.util.Arrays.toString(fileNode.getBlocks()) + ". " +
      "Ensure the fileNode is retrieved from the current FSDirectory after " +
      "any NameNode restart.");
}
```

This improves debuggability by providing:
- Clear indication that `lastBlock` is null
- Explanation of the likely cause (stale reference)
- Current state of the fileNode's blocks array
- Guidance on how to fix the issue

## Pattern

This follows the same pattern as Groups 4, 19, 26, 44, 48, and 53 where tests store references to NameNode components before restart and fail to re-fetch them afterward.
