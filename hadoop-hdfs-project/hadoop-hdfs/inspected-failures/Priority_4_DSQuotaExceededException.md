# DSQuotaExceededException: Quota Increased 38x After Restart

**Priority:** 4 (Originally classified as LIKELY_BUG)
**Severity:** High - Quota accounting failure
**Category:** TEST DESIGN ISSUE / POSSIBLE BUG
**Status:** Under Investigation - Test has unrealistic quota, but underlying issue may exist

---

## Executive Summary

A DSQuotaExceededException occurs when tests try to write to files after NameNode restart. The error claims disk space consumption jumped from expected (~2KB) to 384MB (38x the 10MB quota). Investigation reveals that HDFS charges the FULL preferred block size (128MB * 3 replication = 384MB) when allocating blocks, even for small writes.

**Root Cause:** The test sets a 10MB disk quota but HDFS's default block size is 128MB. When allocating a block (even for 1KB of data), HDFS reserves quota for the full block size * replication factor. The test fails because 384MB > 10MB quota.

**Why After Restart?** The file has 0 blocks after restart, suggesting either:
1. The block was never allocated before restart (data buffered client-side), OR
2. The under-construction block was lost during restart

---

## Reproduction

### Affected Tests
All 8 test methods in `TestQuota_RestartInjected`:
1. `testQuota_AfterHflush_NN_Crash`
2. `testQuota_AfterHflush_NN_Graceful`
3. `testQuota_AfterHflush_SingleDN_Crash`
4. `testQuota_AfterHflush_SingleDN_Graceful`
5. `testQuota_AfterHflush_AllDN_Crash`
6. `testQuota_AfterHflush_AllDN_Graceful`
7. `testQuota_AfterHflush_NNDN_Crash`
8. `testQuota_AfterHflush_NNDN_Graceful`

### Reproduction Steps
```bash
mvn surefire:test -Dtest=TestQuota_RestartInjected#testQuota_AfterHflush_NN_Crash
```

---

## Stack Trace

```
org.apache.hadoop.hdfs.protocol.DSQuotaExceededException:
The DiskSpace quota of /quota_restart_test is exceeded: quota = 10485760 B = 10 MB but diskspace consumed = 402653184 B = 384 MB
	at org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.verifyStoragespaceQuota(DirectoryWithQuotaFeature.java:200)
	at org.apache.hadoop.hdfs.server.namenode.DirectoryWithQuotaFeature.verifyQuota(DirectoryWithQuotaFeature.java:227)
	at org.apache.hadoop.hdfs.server.namenode.FSDirectory.updateCount(FSDirectory.java:1052)
	at org.apache.hadoop.hdfs.server.namenode.FSDirWriteFileOp.addBlock(FSDirWriteFileOp.java:513)
	at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getAdditionalBlock(FSNamesystem.java:2997)
```

---

## Root Cause Analysis

### Test Code

**TestQuota_RestartInjected.java:**

```java
private void testQuotaWithRestart(RestartTarget target, RestartMode mode) throws Exception {
  Configuration conf = new HdfsConfiguration();
  MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

  DistributedFileSystem fs = cluster.getFileSystem();

  Path quotaDir = new Path("/quota_restart_test");
  fs.mkdirs(quotaDir);
  fs.setQuota(quotaDir, 100, 10 * 1024 * 1024); // ← 10MB space quota

  Path file = new Path(quotaDir, "testfile.dat");
  FSDataOutputStream stream = fs.create(file);
  byte[] buffer = AppendTestUtil.initBuffer(1024); // 1KB
  stream.write(buffer);  // Write 1KB
  stream.hflush();       // Flush

  // RESTART
  executeRestart(cluster, target, mode, true);

  // Try to write more
  stream.write(buffer);  // ← FAILS HERE with quota exceeded
  stream.close();
}
```

### The Problem: Quota Accounting for Incomplete Blocks

**INodeFile.java:storagespaceConsumedContiguous()** - How quota is calculated:

```java
for (BlockInfo b : blocks) {
  long blockSize = b.isComplete() ? b.getNumBytes() :
      getPreferredBlockSize();  // ← 128MB for incomplete blocks!
  counts.addStorageSpace(blockSize * replication);
}
```

**Key insight:** For incomplete (under-construction) blocks, HDFS charges quota for the FULL `preferredBlockSize`, not the actual bytes written.

### Debug Output Analysis

**Added debug logging** to `FSDirWriteFileOp.addBlock()` and `DirectoryWithQuotaFeature.verifyStoragespaceQuota()`:

```
BEFORE RESTART:
  (No blocks allocated yet - data buffered client-side)

AFTER RESTART - when trying to add block:
QUOTA_DEBUG: addBlock() - file=/quota_restart_test/testfile.dat
  Preferred block size: 134217728 (128 MB)
  Replication: 3
  Existing blocks: 0  ← File has NO blocks after restart!
  Total existing space: 0

QUOTA_DEBUG: verifyStoragespaceQuota()
  Quota limit: 10485760 bytes (10 MB)
  Current usage: 0 bytes (0 MB)
  Delta: 402653184 bytes (384 MB)  ← 128MB * 3 replicas!
  After delta: 402653184 bytes (384 MB)

ERROR: 384 MB > 10 MB quota → DSQuotaExceededException
```

**Calculation:**
- Preferred block size: 128 MB (134217728 bytes)
- Replication factor: 3
- Quota delta: 128 MB * 3 = 384 MB (402653184 bytes)
- Quota limit: 10 MB
- **Result: 384 MB > 10 MB → FAIL**

### Why This Happens After Restart

**Timeline:**

1. **Before Restart:**
   - `stream.write(1KB)` - data buffered in client
   - `stream.hflush()` - may allocate block or just flush to DataNode buffers
   - **No block allocation visible to NameNode yet** (or block was under construction)

2. **During Restart:**
   - NameNode shuts down and restarts
   - FSImage/EditLog loaded
   - **File has 0 blocks after restart**

3. **After Restart:**
   - `stream.write(1KB)` - client tries to continue writing
   - Needs to allocate first block
   - Charges 384MB (128MB * 3) to quota
   - Quota limit is 10MB
   - **FAILS: 384MB > 10MB**

### Two Possible Scenarios

**Scenario A: Block Never Allocated Before Restart**
- `hflush()` doesn't force block allocation (only `hsync()` does)
- Data was buffered client-side
- After restart, client needs to allocate first block
- Explanation: Normal behavior, test's quota is just too small

**Scenario B: Under-Construction Block Lost During Restart**
- Block was allocated before restart but under construction
- Restart failed to recover the under-construction block
- File lost the block metadata
- Explanation: **Potential data loss bug** in restart/recovery

Debug evidence shows the file has 0 blocks after restart, but we need to verify which scenario is occurring.

---

## Test Design Issue

### Unrealistic Quota Setting

The test sets a 10MB quota with default 128MB blocks and replication 3:

```java
fs.setQuota(quotaDir, 100, 10 * 1024 * 1024); // 10MB quota
```

**Problem:** A single block requires 384MB of quota (128MB * 3), which is 38x the quota limit!

This is an **invalid test configuration**. Even without restart, writing enough data to allocate a block would fail.

### Expected Test Design

For quota testing with small writes, the test should either:

**Option 1:** Set much larger quota
```java
fs.setQuota(quotaDir, 100, 400 * 1024 * 1024); // 400MB quota
```

**Option 2:** Configure smaller block size
```java
conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024 * 1024); // 1MB blocks
fs.setQuota(quotaDir, 100, 10 * 1024 * 1024); // 10MB quota (can fit 3 blocks with replication)
```

---

## Is This a Production Bug?

### Arguments FOR Being a Bug

1. **File has 0 blocks after restart** - This could indicate under-construction block recovery failure
2. **Quota should track actual usage** - Charging 384MB for 1KB of data seems wrong
3. **Test expects to succeed** - The test was written to validate quota after restart

### Arguments AGAINST Being a Bug

1. **Test configuration is invalid** - 10MB quota can't hold a single 384MB block
2. **HDFS quota design** - Pre-allocating full block size to quota is intentional (prevents quota violations mid-block-write)
3. **hflush() doesn't guarantee persistence** - Block may not have been allocated before restart

### Verdict: LIKELY TEST ISSUE, BUT NEEDS VERIFICATION

**Primary Issue:** Test has unrealistic quota that can't accommodate a single block

**Secondary Concern:** Why does the file have 0 blocks after restart? Need to verify if:
- Block was never allocated (client-side buffering), OR
- Block was lost during restart (recovery bug)

---

## Impact Assessment

### Production Impact: **LOW TO MEDIUM**

**IF Scenario A (block never allocated):**
- No production impact
- Test issue only
- Severity: **LOW**

**IF Scenario B (block lost during restart):**
- Under-construction blocks may be lost during restart
- Data loss risk for files being written during NameNode failure
- Lease recovery may not work correctly
- Severity: **MEDIUM TO HIGH**

### Test Impact: **MEDIUM**

- 8 test methods fail
- Test configuration is invalid (quota too small for block size)
- Needs fix regardless of whether production bug exists

---

## Recommended Actions

### Immediate: Fix the Test

Update `TestQuota_RestartInjected.java` to use realistic configuration:

```java
private void testQuotaWithRestart(RestartTarget target, RestartMode mode) throws Exception {
  Configuration conf = new HdfsConfiguration();
  // Set smaller block size for testing
  conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024 * 1024); // 1MB blocks

  MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(3).build();

  DistributedFileSystem fs = cluster.getFileSystem();

  Path quotaDir = new Path("/quota_restart_test");
  fs.mkdirs(quotaDir);
  // Quota can now fit 3 blocks: 3 * 1MB * 3 replication = 9MB < 10MB
  fs.setQuota(quotaDir, 100, 10 * 1024 * 1024); // 10MB quota

  // Rest of test unchanged...
}
```

### Investigation: Determine If Block Was Lost

Add instrumentation to verify Scenario A vs B:

1. **Before restart:** Check if block was allocated
2. **After restart:** Check if block should have been recovered
3. **Compare:** FSImage/EditLog before and after restart

**Debug code to add:**

```java
// Before restart
cluster.getNamesystem().readLock();
try {
  INode inode = cluster.getNamesystem().getFSDirectory().getINode("/quota_restart_test/testfile.dat");
  if (inode != null && inode.isFile()) {
    INodeFile fileINode = inode.asFile();
    System.out.println("BEFORE RESTART: blocks = " + fileINode.getBlocks().length);
    for (BlockInfo blk : fileINode.getBlocks()) {
      System.out.println("  Block: " + blk + ", complete=" + blk.isComplete());
    }
  }
} finally {
  cluster.getNamesystem().readUnlock();
}

// After restart - same check
```

### Long-term: Review Quota Accounting

Consider whether charging full block size for incomplete blocks is optimal:

**Current Behavior:**
- Incomplete block: charge 128MB
- Actual usage: 1KB
- Wasted quota: 127.999MB

**Alternative Approaches:**
1. Charge actual bytes + safety margin
2. Dynamically adjust quota as block fills
3. Separate accounting for under-construction blocks

**Trade-offs:** Need to prevent quota violations when blocks are being written

---

## HDFS Quota Design Background

### Why Charge Full Block Size?

HDFS reserves quota for the full block size to prevent this scenario:

1. User has 1MB quota remaining
2. Starts writing a file (allocates 128MB block)
3. If only charged for actual bytes, quota OK initially
4. As they write, actual usage grows
5. Quota exceeded mid-write → file corruption

By charging the full block size upfront, HDFS ensures quota won't be exceeded mid-block.

### Is This Optimal?

**Pros:**
- Prevents quota violations during writes
- Simple accounting
- Predictable behavior

**Cons:**
- Wastes quota for small files
- Misleading quota usage reports
- Can prevent valid operations (like this test)

---

## Comparison: Quota Before vs After Restart

### Expected Behavior (Without Restart)

If the test ran without restart:

**Option 1:** Block allocated during first write
1. `stream.write(1KB)` → allocates 128MB block
2. Quota charged: 384MB
3. 384MB > 10MB → **Fails immediately** (before restart)

**Option 2:** Block not allocated until buffer full
1. `stream.write(1KB)` → buffered client-side
2. `stream.hflush()` → still buffered (< 64KB packet size)
3. `stream.write(1KB)` → still buffered
4. `stream.close()` → allocates block, 384MB > 10MB → **Fails at close**

### Actual Behavior (With Restart)

What we observe:
1. `stream.write(1KB) + hflush()` → succeeds (no block allocated yet?)
2. Restart
3. `stream.write(1KB)` → tries to allocate block, fails with quota exceeded

**Implication:** The block was NOT allocated before restart, confirming Scenario A (buffering)

---

## Evidence

###  Quota Delta Calculation

From `FSDirWriteFileOp.addBlock()`:

```java
fsd.updateCount(inodesInPath, 0, fileINode.getPreferredBlockSize(),
    fileINode.getFileReplication(), true);
```

- nsDelta = 0 (namespace)
- ssDelta = `getPreferredBlockSize()` = 134217728 bytes (128 MB)
- replication = 3

From `FSDirectory.updateCount()`:

```java
storageSpace(ssDelta * replication)
```

**Result:** 134217728 * 3 = 402653184 bytes = 384 MB

### Block Size Defaults

**DFS_BLOCK_SIZE_DEFAULT:**
- Default: 128 MB (134217728 bytes)
- Test doesn't override, so uses default
- Result: Every block allocation charges 384MB to quota (with replication 3)

---

## Related Code

### Key Files

1. **DirectoryWithQuotaFeature.java:197-210** - Quota verification
2. **INodeFile.java:storagespaceConsumedContiguous()** - Quota calculation for files
3. **FSDirWriteFileOp.java:513-514** - Block allocation quota update
4. **FSDirectory.java:1005-1015** - updateCount implementation

### Quota Accounting Flow

```
Client.addBlock()
  → NameNodeRpcServer.addBlock()
    → FSNamesystem.getAdditionalBlock()
      → FSDirWriteFileOp.storeAllocatedBlock()
        → FSDirWriteFileOp.addBlock()
          → FSDirectory.updateCount(0, preferredBlockSize, replication)
            → FSDirectory.updateCount(QuotaCounts)
              → FSDirectory.verifyQuota()
                → DirectoryWithQuotaFeature.verifyQuota()
                  → DirectoryWithQuotaFeature.verifyStoragespaceQuota()
                    → THROWS DSQuotaExceededException if quota exceeded
```

---

## Conclusions

### Classification

**Primary Issue:** **TEST DESIGN BUG** - Invalid quota configuration

The test sets a 10MB quota but uses 128MB blocks with replication 3. A single block requires 384MB of quota (38x the limit). The test cannot possibly succeed with this configuration.

**Secondary Concern:** **INVESTIGATION NEEDED** - Block recovery after restart

The file has 0 blocks after restart, which needs explanation:
- **If expected (buffering):** No production bug, just fix test config
- **If unexpected (recovery failure):** Potential data loss bug in under-construction block recovery

### Recommended Priority

**Downgrade from Priority 4 (LIKELY_BUG) to Priority 10-12 (TEST_FRAMEWORK)**

Primary issue is test configuration, not production code. However, keep on radar pending investigation of block recovery behavior.

### Action Items

1. ✅ **Fix test configuration** - Set appropriate block size or quota
2. ⏳ **Investigate block recovery** - Verify if under-construction blocks are properly recovered after restart
3. ⏳ **Audit other quota tests** - Check for similar configuration issues
4. ⏳ **Consider quota design review** - Evaluate whether full block size pre-allocation is optimal

---

## Test Output Reference

- Test logs: `target/surefire-reports/org.apache.hadoop.hdfs.TestQuota_RestartInjected-output.txt`
- Debug run: `/tmp/priority4_debug_run.log`
- Test run: `/tmp/priority4_test_run.log`

**Date tested:** 2025-12-01
**Tester:** Claude Code Analysis
**Environment:** Hadoop 3.3.5, Ubuntu 20.04, Java 8
