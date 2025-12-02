# CannotObtainBlockLengthException: Cannot Obtain Block Length for Under-Construction Blocks

**Priority:** 6 (Originally classified as LIKELY_BUG)
**Severity:** High - Tests fail after restart
**Category:** LIKELY BUG / RESTART TIMING ISSUE
**Status:** Under Investigation - Potential lease recovery or block finalization bug

---

## Executive Summary

CannotObtainBlockLengthException occurs in 6 test methods when trying to open files for reading after restart. The files have under-construction (UC) blocks that were flushed (`hflush()`) but not closed before restart. After restart, DataNodes cannot provide the visible length of these blocks.

**Root Cause:** After restart, DataNodes that hold replicas of under-construction blocks cannot provide the block's visible length via `getReplicaVisibleLength()`. This causes `DFSInputStream.readBlockLength()` to fail when opening the file for reading.

**Classification:** **LIKELY BUG** or **RESTART POINT TIMING ISSUE**

**Potential Issues:**
1. **Lease recovery failure**: Under-construction blocks may not be properly recovered after restart
2. **Block finalization failure**: Blocks remain in UC state when they should be finalized
3. **DataNode replica state inconsistency**: Replicas not properly updated after restart
4. **Test timing issue**: Tests open files too soon after restart, before lease recovery completes

---

## Reproduction

### Affected Tests (All 6 tests)

**TestBlockToken_RestartInjected (6 tests):**
1. `testLastLocatedBlockTokenExpiry_AfterHflush_AllDN_Crash`
2. `testLastLocatedBlockTokenExpiry_AfterHflush_AllDN_Graceful`
3. `testLastLocatedBlockTokenExpiry_AfterHflush_NNDN_Crash`
4. `testLastLocatedBlockTokenExpiry_AfterHflush_NNDN_Graceful`
5. `testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Crash`
6. `testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Graceful`

**Similar issues in other test classes:**
- TestClientProtocolForPipelineRecovery_RestartInjected (6 tests) - Error Group #9
- TestFileConcurrentReader_RestartInjected (12 tests) - Error Groups #12, #13
- TestFileAppend2_RestartInjected (5 tests) - Error Group #15

### Reproduction Steps

```bash
mvn surefire:test -Dtest=TestBlockToken_RestartInjected#testLastLocatedBlockTokenExpiry_AfterHflush_SingleDN_Crash
```

---

## Stack Trace

```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException: Cannot obtain block length for LocatedBlock{BP-1880036437-127.0.1.1-1764618479418:blk_1073741825_1001; getBlockSize()=6; corrupt=false; offset=0; locs=[DatanodeInfoWithStorage[127.0.0.1:35933,DS-d38653ee-6e25-4523-97ee-aef6247713b5,DISK]]; cachedLocs=[]} of /tmp/abc.log
	at org.apache.hadoop.hdfs.DFSInputStream.readBlockLength(DFSInputStream.java:414)
	at org.apache.hadoop.hdfs.DFSInputStream.getLastBlockLength(DFSInputStream.java:323)
	at org.apache.hadoop.hdfs.DFSInputStream.openInfo(DFSInputStream.java:243)
	at org.apache.hadoop.hdfs.DFSInputStream.<init>(DFSInputStream.java:213)
	at org.apache.hadoop.hdfs.DFSClient.openInternal(DFSClient.java:1084)
	at org.apache.hadoop.hdfs.DFSClient.open(DFSClient.java:1047)
	at org.apache.hadoop.hdfs.DistributedFileSystem.open(DistributedFileSystem.java:340)
	at org.apache.hadoop.fs.FileSystem.open(FileSystem.java:997)
	at TestBlockToken_RestartInjected.testLastLocatedBlockTokenExpiryWithRestart:116
```

---

## Root Cause Analysis

### Test Sequence

**TestBlockToken_RestartInjected.java:**

```java
private void testLastLocatedBlockTokenExpiryWithRestart(RestartTarget target, RestartMode mode) {
  Configuration conf = new Configuration();
  conf.setBoolean(DFSConfigKeys.DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);

  try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build()) {
    cluster.waitClusterUp();

    // Setup block token lifetime
    final BlockTokenSecretManager sm = ...;
    SecurityTestUtil.setBlockTokenLifetime(sm, 1000L);

    DistributedFileSystem fs = cluster.getFileSystem();
    Path p = new Path("/tmp/abc.log");
    FSDataOutputStream out = fs.create(p);

    // Write 6 bytes
    byte[] data = "hello\n".getBytes(StandardCharsets.UTF_8);  // 6 bytes
    out.write(data);
    out.hflush();  // ← Flush to DataNode, block is still UNDER CONSTRUCTION

    // === RESTART HERE ===
    executeRestart(cluster, target, mode, true);
    verifyClusterHealth(cluster, fs);

    // Re-set block token lifetime after NN restart
    if (target == RestartTarget.NAMENODE || target == RestartTarget.NAMENODE_AND_DATANODES) {
      final BlockTokenSecretManager smAfter = ...;
      SecurityTestUtil.setBlockTokenLifetime(smAfter, 1000L);
    }

    // ❌ PROBLEM: Try to open file for reading
    FSDataInputStream in = fs.open(p);  // LINE 115 - FAILS!
    out.close();

    // Test continues...
  }
}
```

### What Happens During File Open

**DFSInputStream.readBlockLength()** is called to get the visible length of the last (under-construction) block:

```java
private long readBlockLength(LocatedBlock locatedblock) throws IOException {
  int replicaNotFoundCount = locatedblock.getLocations().length;

  LinkedList<DatanodeInfo> nodeList = new LinkedList<>(
      Arrays.asList(locatedblock.getLocations()));

  while (nodeList.size() > 0) {
    DatanodeInfo datanode = nodeList.pop();
    ClientDatanodeProtocol cdp = null;
    try {
      cdp = DFSUtilClient.createClientDatanodeProtocolProxy(datanode, ...);

      // Try to get visible length from DataNode
      final long n = cdp.getReplicaVisibleLength(locatedblock.getBlock());

      if (n >= 0) {
        return n;
      }
    } catch (IOException ioe) {
      if (ioe instanceof RemoteException) {
        if (((RemoteException) ioe).unwrapRemoteException() instanceof
            ReplicaNotFoundException) {
          // Replica not found on this DN
          replicaNotFoundCount--;
        }
      }
      // Log and try next DataNode
    }
  }

  // If all DataNodes failed or replica not found on all
  if (replicaNotFoundCount == 0) {
    return 0;  // All DNs reported replica not found
  }

  // Cannot obtain block length from any DataNode
  throw new CannotObtainBlockLengthException(locatedblock, src);
}
```

**The Problem:**

After restart, when the client calls `cdp.getReplicaVisibleLength()` on the DataNode:
1. DataNode doesn't have the replica (if DataNode was restarted)
2. DataNode has the replica but it's in an invalid state
3. DataNode can't provide visible length for under-construction blocks after restart
4. Returns -1 or throws ReplicaNotFoundException

Since all DataNodes fail to provide the visible length, the exception is thrown.

---

## Comparison with Original Test

**Original Test (No Restart) - TestBlockToken.java:**

```java
@Test
public void testLastLocatedBlockTokenExpiry() throws IOException, InterruptedException {
  Configuration conf = new Configuration();
  conf.setBoolean(DFSConfigKeys.DFS_BLOCK_ACCESS_TOKEN_ENABLE_KEY, true);

  try (MiniDFSCluster cluster = new MiniDFSCluster.Builder(conf).numDataNodes(1).build()) {
    cluster.waitClusterUp();

    final BlockTokenSecretManager sm = ...;
    SecurityTestUtil.setBlockTokenLifetime(sm, 1000L);

    DistributedFileSystem fs = cluster.getFileSystem();
    Path p = new Path("/tmp/abc.log");
    FSDataOutputStream out = fs.create(p);
    byte[] data = "hello\n".getBytes(StandardCharsets.UTF_8);
    out.write(data);
    out.hflush();

    // ✅ ORIGINAL: Open file BEFORE any restart
    FSDataInputStream in = fs.open(p);
    out.close();

    Thread.sleep(2000L);

    byte[] readData = new byte[data.length];
    in.read(readData);  // ← Works fine, no restart occurred
  }
}
```

**Key Difference:**
- **Original test**: Opens file for reading immediately after `hflush()`, no restart
- **Restart-injected test**: Restarts after `hflush()`, then tries to open file
- **Result**: Original works, restart-injected fails

**This indicates a problem with how under-construction blocks are handled across restarts.**

---

## Why This Matters in Production

### Under-Construction Blocks in Production

In HDFS, a file can have blocks in different states:
1. **Complete**: Block is finalized, all writes complete
2. **Under Construction (UC)**: Block is being written to, not finalized yet
3. **Under Recovery**: Block is being recovered (e.g., after pipeline failure)

**Common scenario:**
1. Application writes to file
2. Calls `hflush()` to persist data to DataNodes
3. Continues writing (file remains open, block UC)
4. **NameNode or DataNode crashes**
5. HDFS performs lease recovery to finalize UC blocks
6. Application or another client tries to read the file

### Expected Behavior After Restart

**Lease Recovery Process:**
1. When NameNode restarts, it loads FSImage and EditLogs
2. Finds files with UC blocks that have no active writer (lease expired)
3. Initiates lease recovery to finalize these blocks
4. Contacts DataNodes to get actual visible length
5. Finalizes blocks with recovered length

**If this process fails or is incomplete:**
- UC blocks remain in UC state
- Readers cannot access the blocks
- **Data may be inaccessible even though it was flushed to DataNodes**

---

## Investigation: Potential Root Causes

### Hypothesis 1: Lease Recovery Not Triggered

**Scenario:**
- File still has valid lease after restart (client connection kept alive)
- Lease not expired, so no recovery triggered
- Block remains in UC state indefinitely
- Cannot be read because it's still "being written"

**Evidence:**
- Test sets: `conf.setInt(DFSConfigKeys.IPC_CLIENT_CONNECTION_MAXIDLETIME_KEY, 0);`
- This keeps client connection alive across NameNode restart
- Lease might still be valid

**If true, this is expected behavior**, not a bug. But test should then close the file before trying to read.

### Hypothesis 2: DataNode Restart Loses Replica State

**Scenario (when DataNode is restarted):**
- DataNode has UC block replica before restart
- After restart, DataNode scans local storage
- UC block replica might not be recognized or loaded properly
- `getReplicaVisibleLength()` returns ReplicaNotFoundException

**Evidence:**
- All 6 failing tests include DataNode restart (SingleDN, AllDN, or NNDN)
- NameNode-only restart tests might not fail (need to verify)

**If true, this could be a bug** in DataNode UC block recovery.

### Hypothesis 3: Lease Recovery Incomplete

**Scenario:**
- Lease recovery is triggered after restart
- But `verifyClusterHealth()` doesn't wait for it to complete
- Test tries to open file before recovery finishes
- Block still in UC state, cannot get visible length

**If true, this is a test timing issue**, not a production bug (recovery would eventually complete).

### Hypothesis 4: Block Finalization Bug

**Scenario:**
- UC block should be finalized after `hflush()` and restart
- But finalization logic has a bug
- Block stays in UC state permanently
- Cannot be read

**If true, this is a serious production bug** - data loss after restart.

---

## Evidence Analysis

### File State After Restart

Looking at the error message:
```
LocatedBlock{...; getBlockSize()=6; corrupt=false; offset=0; locs=[...]}
```

**Key observations:**
- `getBlockSize()=6` - Block size is correct (6 bytes = "hello\n")
- `corrupt=false` - Block not marked as corrupt
- `locs=[...]` - NameNode knows which DataNode has the replica
- **NameNode has the block metadata**, but DataNode can't provide visible length

This suggests:
1. ✅ Block metadata recovered by NameNode from FSImage/EditLog
2. ✅ NameNode knows block size (6 bytes)
3. ✅ NameNode knows replica location
4. ❌ DataNode cannot provide visible length via RPC

### DataNode Restart Impact

**Test variants and expected behavior:**

| Restart Target | NameNode Restarts? | DataNode Restarts? | Expected Impact |
|----------------|--------------------|--------------------|-----------------|
| SingleDN_Crash | No | Yes (1 DN) | DN loses UC block state |
| SingleDN_Graceful | No | Yes (1 DN) | DN should preserve state |
| AllDN_Crash | No | Yes (all DNs) | All DNs lose UC block state |
| AllDN_Graceful | No | Yes (all DNs) | DNs should preserve state |
| NNDN_Crash | Yes | Yes | Both lose state |
| NNDN_Graceful | Yes | Yes | Should preserve state |

**All 6 tests fail**, including graceful shutdowns where state should be preserved.

**Conclusion:** This suggests a systematic issue with UC block handling after restart, not just crash-related state loss.

---

## Is This a Production Bug?

### Arguments FOR Being a Bug

1. **Data inaccessibility**: Flushed data cannot be read after restart
2. **Affects multiple restart scenarios**: Graceful shutdowns also fail
3. **Lease recovery should work**: HDFS is designed to handle this case
4. **Could cause data loss perception**: Users see "cannot read file" even though data was persisted

### Arguments AGAINST Being a Bug

1. **File not closed**: Output stream still open, file legitimately under construction
2. **Test timing**: May need to wait for lease recovery to complete
3. **Original test doesn't read after flush**: Original closes output before reading final data

### Verdict: **LIKELY BUG** or **REQUIRES INVESTIGATION**

**This likely indicates one of:**
1. **Lease recovery bug**: Recovery not completing properly after restart
2. **Block finalization bug**: UC blocks not being finalized when they should be
3. **DataNode replica recovery bug**: UC replicas not properly recovered after DataNode restart

**Recommended:** Further investigation needed to determine exact cause.

---

## Impact Assessment

### Production Impact: **MEDIUM TO HIGH** (if bug confirmed)

**If this is a genuine bug:**
- Files being written during crashes may become unreadable
- Flushed data could be inaccessible
- Applications would see failures when trying to read in-progress files after recovery
- Could affect HBase WALs, Kafka logs, streaming applications

**Severity depends on:**
- How quickly lease recovery completes (if it does eventually)
- Whether workarounds exist (e.g., wait for lease expiry)
- Frequency of the scenario (writing during restart)

### Test Impact: **HIGH**

- 6 tests fail in TestBlockToken_RestartInjected
- Similar failures in 23 other tests (Error Groups #9, #12, #13, #15)
- Total: **29 test methods affected** across multiple test classes
- Large number of false failures if this is expected behavior

---

## Recommended Actions

### Immediate: Investigate Root Cause

1. **Add detailed logging** to understand what's happening:

```java
// In test, before fs.open()
FSDataOutputStream out = fs.create(p);
out.write(data);
out.hflush();

LOG.info("Before restart - file status: " + fs.getFileStatus(p));
LOG.info("Before restart - is file UC: " +
    cluster.getNamesystem().isFileUnderConstruction(p));

executeRestart(cluster, target, mode, true);
verifyClusterHealth(cluster, fs);

LOG.info("After restart - file status: " + fs.getFileStatus(p));
LOG.info("After restart - is file UC: " +
    cluster.getNamesystem().isFileUnderConstruction(p));
LOG.info("After restart - lease info: " +
    cluster.getNamesystem().getLeaseManager().getLeaseByPath(p.toString()));

// Try to open
try {
  FSDataInputStream in = fs.open(p);
  LOG.info("Successfully opened file");
} catch (CannotObtainBlockLengthException e) {
  LOG.error("Failed to open file", e);
  // Log block state from NameNode
  // Log replica state from DataNode
}
```

2. **Check DataNode replica state** after restart:

```java
// Query DataNode directly
DataNode dn = cluster.getDataNodes().get(0);
ReplicaInfo replica = dn.getFSDataset().getReplica(block);
if (replica != null) {
  LOG.info("Replica state: " + replica.getState());
  LOG.info("Replica bytes: " + replica.getNumBytes());
  LOG.info("Replica visible length: " + replica.getBytesOnDisk());
}
```

3. **Test lease recovery** explicitly:

```java
// Force lease recovery
cluster.getNamesystem().recoverLease(p.toString(), null);
// Wait for recovery
Thread.sleep(5000);
// Try to open
FSDataInputStream in = fs.open(p);
```

### Short-term: Fix Tests or Code

**Option A: If this is expected behavior (file still UC)**

Close the output stream before restart or before opening for read:

```java
out.write(data);
out.hflush();

executeRestart(cluster, target, mode, true);

// Close output stream to finalize block
out.close();

// Now open for reading
FSDataInputStream in = fs.open(p);
```

**Option B: If this is a bug in production code**

Fix the lease recovery or block finalization logic to properly handle UC blocks after restart.

### Long-term: Audit UC Block Handling

1. Review lease recovery implementation
2. Review block finalization logic after restart
3. Review DataNode UC replica recovery
4. Add integration tests for UC block recovery scenarios
5. Document expected behavior clearly

---

## Questions to Answer

1. **Is the file supposed to be readable while output stream is open?**
   - Original test opens for read before closing output
   - Suggests it should work

2. **Does lease recovery run after restart?**
   - Check NameNode logs for lease recovery messages
   - Verify LeaseManager state

3. **Why can't DataNode provide visible length?**
   - Check DataNode logs for errors
   - Examine replica state on disk
   - Test `getReplicaVisibleLength()` RPC directly

4. **Does waiting help?**
   - Add sleep before opening file
   - See if lease recovery completes eventually

5. **Do NameNode-only restarts also fail?**
   - Test with RestartTarget.NAMENODE only
   - If they work, problem is DataNode-specific

---

## Conclusion

This issue is **classified as LIKELY BUG** pending further investigation.

**Evidence suggests:**
- Under-construction blocks are not properly handled after restart
- DataNodes cannot provide visible length for UC blocks after restart
- May be a lease recovery bug, block finalization bug, or test timing issue

**Next Steps:**
1. Add detailed logging to understand block/replica state
2. Test lease recovery explicitly
3. Determine if this is a production bug or test issue
4. Fix either the code or the tests based on findings

**Priority should remain high** until investigation confirms this is not a data accessibility bug.

---

## Test Output Reference

- Test run: `/tmp/priority6_test_run.log`
- Original batch run: `restart-test-log-20251130_220042/TestBlockToken_RestartInjected.log`

**Date tested:** 2025-12-01
**Tester:** Claude Code Analysis
**Environment:** Hadoop 3.3.5, Ubuntu 20.04, Java 8
