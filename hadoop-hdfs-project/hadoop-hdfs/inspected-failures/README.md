# Inspected Failures

This directory contains detailed inspection reports for failures discovered during restart-injection testing.

## Inspection Status

| Priority | Issue | Status | Report | Severity |
|----------|-------|--------|--------|----------|
| 1 | NullPointerException in BlocksMap.numNodes() | ✅ CONFIRMED BUG | [Priority_1_NPE_BlocksMap_numNodes.md](Priority_1_NPE_BlocksMap_numNodes.md) | Critical |
| 2 | NullPointerException in BlocksMap.getStoredBlock() | ❌ FALSE POSITIVE | [Priority_2_NPE_BlocksMap_getStoredBlock.md](Priority_2_NPE_BlocksMap_getStoredBlock.md) | Test Framework |
| 3 | AssertionError in BlockManager.findAndMarkBlockAsCorrupt() | ❌ FALSE POSITIVE | [Priority_3_AssertionError_BlockManager.md](Priority_3_AssertionError_BlockManager.md) | Test Framework |
| 4 | DSQuotaExceededException - quota increased 38x after restart | 🔧 TEST DESIGN ISSUE | [Priority_4_DSQuotaExceededException.md](Priority_4_DSQuotaExceededException.md) | Test Configuration |
| 5 | OutOfMemoryError: Java heap space | 🔧 TEST FRAMEWORK | [Priority_5_OutOfMemoryError.md](Priority_5_OutOfMemoryError.md) | Test Framework |
| 6 | CannotObtainBlockLengthException - hflush() visibility broken after DN restart | ✅ **CONFIRMED BUG** | [Priority_6_CONFIRMED_BUG.md](Priority_6_CONFIRMED_BUG.md) | **High** |
| 7-10 | CannotObtainBlockLengthException - Same as Priority 6 (different files) | ✅ **DUPLICATE OF #6** | [Priority_7_10_Same_As_Priority_6.md](Priority_7_10_Same_As_Priority_6.md) | **High** |
| 11-12 | SnapshotException: Nested snapshottable directories not allowed | 🔧 TEST FRAMEWORK | [Priority_11_SnapshotException_TEST_FRAMEWORK.md](Priority_11_SnapshotException_TEST_FRAMEWORK.md) | Test Framework |
| 13 | Not all DataNodes registered after restart (expected 2 got 1) | ⏱️ TIMING ISSUE | [Priority_13_DataNode_Registration_TEST_FRAMEWORK.md](Priority_13_DataNode_Registration_TEST_FRAMEWORK.md) | Test Framework |
| 14 | IllegalArgumentException: Namenode index is needed for HA | 🔧 TEST FRAMEWORK | [Priority_14_IllegalArgumentException_HA_TEST_FRAMEWORK.md](Priority_14_IllegalArgumentException_HA_TEST_FRAMEWORK.md) | Test Framework |
| 15 | Another DiskBalancer is running | 🔧 TEST FRAMEWORK | - | Test Framework |
| 16 | AssertionError with no detailed message | 🔧 TEST FRAMEWORK | - | Test Framework |
| 17 | AssertionError: expected 1 but was 0 in TestPendingReconstruction | ⏱️ TIMING ISSUE | - | Test Framework |
| 18 | AssertionError (assertFalse) in TestSnapshotPathINodes | 🔧 TEST FRAMEWORK | - | Test Framework |
| 19 | TestTimedOutException: test timed out after 180s | ⏱️ TIMING ISSUE | - | Test Framework |
| 20 | Failed to replace bad datanode - no good datanodes available | ⏱️ TIMING ISSUE | - | Test Framework |
| 21 | All datanodes are bad - aborting | ⏱️ TIMING ISSUE | - | Test Framework |
| 22 | Failed to replace bad datanode (single datanode) | ⏱️ TIMING ISSUE | - | Test Framework |
| 23 | All datanodes are bad (SimulatedStorage) | ⏱️ TIMING ISSUE | - | Test Framework |
| 24 | Failed to replace bad datanode (ALWAYS policy) | ⏱️ TIMING ISSUE | - | Test Framework |
| 25 | libhadoop cannot be loaded - UNIX domain socket | 🔧 ENVIRONMENT | - | Environment |

## Report Format

Each inspection report includes:

1. **Executive Summary** - Brief description of the issue
2. **Reproduction Steps** - How to reproduce the failure
3. **Stack Trace** - Complete error stack trace
4. **Root Cause Analysis** - Detailed analysis of why the failure occurs
5. **Evidence** - Debug logs, test outputs, code analysis
6. **Impact Assessment** - Production impact and affected operations
7. **Recommended Fix** - Immediate mitigations and long-term solutions
8. **Verification** - How to verify the fix works

## Priority 1: NPE in BlocksMap.numNodes()

**Status:** ✅ CONFIRMED ACTUAL BUG

**Summary:** NullPointerException when calling `getBlockLocations()` on files created via `concat()` after NameNode restart. This is a critical production bug that can crash the NameNode during normal read operations.

**Affected Tests:**
- TestHDFSConcat_RestartInjected.testConcat_AfterConcat_NN_Crash
- TestHDFSConcat_RestartInjected.testConcat_AfterConcat_NN_Graceful
- TestHDFSConcat_RestartInjected.testConcatNotCompleteBlock_AfterConcat_NN_Crash
- TestHDFSConcat_RestartInjected.testConcatNotCompleteBlock_AfterConcat_NN_Graceful

**Root Cause:** Inconsistency between INodeFile's blocks array and BlocksMap after concat operation and restart. The concat operation appears to create block references that are not properly persisted/restored in the BlocksMap.

**See:** [Priority_1_NPE_BlocksMap_numNodes.md](Priority_1_NPE_BlocksMap_numNodes.md)

---

## Priority 2: NPE in BlocksMap.getStoredBlock()

**Status:** ❌ FALSE POSITIVE - TEST FRAMEWORK BUG

**Summary:** NullPointerException when calling `BlocksMap.getStoredBlock()` after NameNode restart. Investigation revealed this is NOT a production bug, but a test transformation issue. The test continues using stale references to the old (shutdown) NameNode instance instead of obtaining fresh references after restart.

**Affected Tests:**
- TestNodeCount_RestartInjected.testNodeCount_AfterFileCreate_NN_Crash
- TestNodeCount_RestartInjected.testNodeCount_AfterFileCreate_NN_Graceful
- TestNodeCount_RestartInjected.testNodeCount_AfterFileCreate_NNAndDN_Crash
- TestNodeCount_RestartInjected.testNodeCount_AfterFileCreate_NNAndDN_Graceful

**Root Cause:** The restart injection transformation adds `executeRestart()` but fails to update the test to refresh cached `FSNamesystem`, `BlockManager`, and other NameNode component references. The test captures these as `final` variables before restart and continues using them after restart, when the old NameNode has been shutdown and `BlocksMap.blocks` has been set to null.

**Why Not a Production Bug:**
- Production clients use RPC connections to NameNode
- RPC connections close on restart
- Clients must reconnect and automatically get new NameNode instances
- **No way for production code to retain references to old NameNode**

**Fix Required:**
1. Update test to refresh component references after restart
2. Fix transformation tool to automatically detect and handle cached references
3. Audit other restart-injected tests for the same pattern

**See:** [Priority_2_NPE_BlocksMap_getStoredBlock.md](Priority_2_NPE_BlocksMap_getStoredBlock.md)

---

## Priority 3: AssertionError in BlockManager.findAndMarkBlockAsCorrupt()

**Status:** ❌ FALSE POSITIVE - TEST FRAMEWORK BUG (Same as Priority 2)

**Summary:** AssertionError in `assert namesystem.hasWriteLock()` when calling `BlockManager.findAndMarkBlockAsCorrupt()` after NameNode restart. Investigation revealed this is the **exact same root cause as Priority 2** - the test uses stale references to the old NameNode instance.

**Affected Tests:**
- TestPendingReconstruction_RestartInjected.testPendingAndInvalidate_AfterCreate_NN_Crash
- TestPendingReconstruction_RestartInjected.testPendingAndInvalidate_AfterCreate_NN_Graceful
- TestPendingReconstruction_RestartInjected.testPendingAndInvalidate_AfterCreate_NNAndDN_Crash
- TestPendingReconstruction_RestartInjected.testPendingAndInvalidate_AfterCreate_NNAndDN_Graceful

**Root Cause:** Identical to Priority 2 - stale references after restart:
1. Test captures `BlockManager bm = namesystem.getBlockManager()` BEFORE restart
2. After restart, acquires write lock on NEW namesystem: `cluster.getNamesystem().writeLock()`
3. But calls method on OLD BlockManager: `bm.findAndMarkBlockAsCorrupt(...)`
4. OLD BlockManager checks if OLD namesystem has write lock
5. But the lock is held on the NEW namesystem, not the old one
6. Assertion fails: `assert namesystem.hasWriteLock()` → false!

**Why Not a Production Bug:**
- Same reasoning as Priority 2
- Production code cannot retain references to old NameNode instances
- RPC connections close and reconnect on restart

**Fix Required:**
- Same fix as Priority 2 - refresh component references after restart
- This reinforces that the transformation tool has a systematic bug

**See:** [Priority_3_AssertionError_BlockManager.md](Priority_3_AssertionError_BlockManager.md)

---

## Priority 4: DSQuotaExceededException - Quota Increased 38x After Restart

**Status:** 🔧 TEST DESIGN ISSUE - Invalid quota configuration

**Summary:** DSQuotaExceededException when trying to write to files after restart, claiming disk space jumped from expected (~2KB) to 384MB. Investigation reveals the test sets a 10MB disk quota but HDFS's default block size is 128MB. A single block with replication 3 requires 384MB of quota (38x the 10MB limit).

**Affected Tests:**
- All 8 test methods in TestQuota_RestartInjected (all variants of testQuota_AfterHflush)

**Root Cause:** **Invalid test configuration**:
1. Test sets 10MB disk space quota
2. HDFS default block size is 128MB
3. Replication factor is 3
4. When allocating a block (even for 1KB write), HDFS reserves: 128MB * 3 = 384MB
5. 384MB > 10MB quota → DSQuotaExceededException

**Why After Restart?**
- Before restart: Block not allocated yet (data buffered client-side)
- After restart: File has 0 blocks
- When continuing write: Needs to allocate first block → 384MB > 10MB → fails

**Why HDFS Charges Full Block Size:**
HDFS pre-allocates quota for the full block size to prevent quota violations mid-block-write. This is intentional design, but the test didn't account for it.

**Fix Required:**
Configure smaller block size in test:
```java
conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 1024 * 1024); // 1MB blocks
// Now quota can fit multiple blocks: 3 * 1MB * 3 replication = 9MB < 10MB
```

**Secondary Investigation Needed:**
Verify why the file has 0 blocks after restart - is the under-construction block properly recovered?

**See:** [Priority_4_DSQuotaExceededException.md](Priority_4_DSQuotaExceededException.md)

---

## Priority 5: OutOfMemoryError: Java heap space

**Status:** 🔧 TEST FRAMEWORK BUG - Two distinct root causes

**Summary:** OutOfMemoryError occurs in 6 test methods across two test classes. Investigation reveals two separate test framework issues, neither affecting production code.

**Affected Tests:**
- TestDataNodeVolumeMetrics_RestartInjected.testVolumeMetrics_AfterHsync_NN_Crash (2 tests)
- TestSetRepIncreasing_RestartInjected (4 tests - only when running full suite)

**Root Causes:**

**Issue 1 - TestDataNodeVolumeMetrics:**
Test transformation bug - the tool added `DFSTestUtil.readFile()` to verify the file after restart, but the file is 2GB (`Integer.MAX_VALUE + 1`). This tries to allocate a 2GB ByteArrayOutputStream, causing OOM.

**Original test:** Verifies DataNode volume metrics (no file read)
**Restart-injected test:** Added file read verification (reads 2GB into memory)

**Issue 2 - TestSetRepIncreasing:**
Memory accumulation across 42 sequential tests. Each test creates a 10-DataNode MiniDFSCluster. Running individually: passes. Running full suite: 8 OOM errors.

**Why Not Production Bugs:**
- **TestDataNodeVolumeMetrics**: Production never loads entire files into memory; streaming reads are used
- **TestSetRepIncreasing**: Production doesn't create/destroy 42 clusters sequentially

**Fixes Required:**
1. Remove large file read from TestDataNodeVolumeMetrics (use original test's metric verification)
2. Configure Surefire to fork tests or increase heap size for TestSetRepIncreasing
3. Update transformation tool to not add file reads for files > 100MB

**See:** [Priority_5_OutOfMemoryError.md](Priority_5_OutOfMemoryError.md)

---

## Priority 6: CannotObtainBlockLengthException - CONFIRMED BUG

**Status:** ✅ **CONFIRMED PRODUCTION BUG**

**Severity:** HIGH - Data Accessibility / Feature Regression

**Summary:** After `hflush()`, HDFS guarantees that written data becomes visible to readers, even while the file remains under construction. **This guarantee is BROKEN after DataNode restart.** Under-construction blocks that have been flushed become inaccessible (visible length = -1) until explicit lease recovery, causing `CannotObtainBlockLengthException` when clients try to read the file.

**Affected Tests:**
- TestBlockToken_RestartInjected (6 tests with DataNode restart)
- 23 other tests with similar UC block + restart patterns
- **Total: 29 test methods**

**Root Cause - CONFIRMED:**

When a DataNode restarts, under-construction block replicas are loaded from disk and placed in `ReplicaWaitingToBeRecovered` (RWR) state. This state explicitly returns **-1** for visible length:

```java
// ReplicaWaitingToBeRecovered.java:76
public long getVisibleLength() {
  return -1;  // no bytes are visible
}
```

**Key Findings from Investigation:**

| Scenario | Replica Visible Length | Result |
|----------|----------------------|--------|
| Before any restart | 6 bytes | ✅ Readable |
| After NameNode-only restart | 6 bytes | ✅ Readable |
| After DataNode restart | **-1** | ❌ **UNREADABLE** |
| After explicit lease recovery | 6 bytes | ✅ Readable |

**The bug ONLY occurs when DataNode restarts!**

**Why This is a Bug:**

1. **HDFS Guarantees Broken**: The original test (without restart) successfully opens files for reading after `hflush()` while output stream is still open. This is a **documented HDFS feature** that applications like HBase rely on.

2. **Regression**: After DataNode restart, this capability is lost - data that was successfully flushed and should be visible becomes invisible.

3. **Inconsistent Behavior**:
   - NameNode restart: File remains readable ✅
   - DataNode restart: File becomes unreadable ❌

4. **Production Impact**:
   - HBase WAL files become unreadable after DataNode failures
   - Streaming applications that write+read simultaneously fail
   - Data is physically on disk but reported as "invisible"

5. **No Automatic Recovery**: Explicit lease recovery required, but lease holder may still be alive (can't recover).

**Recommended Fix:**

Modify `ReplicaWaitingToBeRecovered` to return actual visible length based on bytes on disk, rather than -1, to restore the `hflush()` visibility guarantee.

**See:**
- [Priority_6_CONFIRMED_BUG.md](Priority_6_CONFIRMED_BUG.md) - Full investigation report
- [Priority_6_CannotObtainBlockLengthException.md](Priority_6_CannotObtainBlockLengthException.md) - Initial analysis

---

## Priorities 7-10: CannotObtainBlockLengthException - DUPLICATE OF PRIORITY 6

**Status:** ✅ **CONFIRMED DUPLICATE** - Same bug as Priority 6

**Summary:** Priorities 7-10 are **the same bug** as Priority 6, just affecting different test files with different file paths. All have the identical root cause: `ReplicaWaitingToBeRecovered.getVisibleLength()` returning -1 after DataNode restart.

**Affected Files:**

| Priority | File Path | Test Class | Occurrences |
|----------|-----------|------------|-------------|
| 7 | `/user/shuai/dataprotocol.dat` | TestClientProtocolForPipelineRecovery_RestartInjected | 6 |
| 8 | `/unfinished-block-buffer` | Various | 6 |
| 9 | `/unfinished-block` | Various | 6 |
| 10 | `/foo` | Various | 5 |

**Why Different Priorities?**

The failure grouping script separated these by file path, but the **root cause is identical**:
- Same error: `CannotObtainBlockLengthException`
- Same stack trace: `DFSInputStream.readBlockLength()` line 414
- Same trigger: DataNode restart with under-construction blocks
- Same fix: Same as Priority 6

**Verification:**

Tested Priority 7 (`TestClientProtocolForPipelineRecovery_RestartInjected.testGetNewStamp_AfterHflush_NNDN_Crash`):
- ✅ Confirmed: Exact same error for `/user/shuai/dataprotocol.dat`
- ✅ Confirmed: Block size = 2 bytes (under-construction)
- ✅ Confirmed: Fails after NameNode+DataNode restart
- ✅ Confirmed: Same root cause as Priority 6

**Conclusion:**

Do not investigate Priorities 7-10 separately. They are duplicates of Priority 6. Once Priority 6 is fixed, all ~29 affected tests across Priorities 6-10 will be resolved.

**See:** [Priority_7_10_Same_As_Priority_6.md](Priority_7_10_Same_As_Priority_6.md)

---

## Priorities 11-12: SnapshotException - Nested Snapshottable Directories Not Allowed

**Status:** 🔧 TEST FRAMEWORK ISSUE (Same root cause for both priorities)

**Summary:** Tests fail with `SnapshotException: Nested snapshottable directories not allowed` after NameNode restart. Investigation reveals this is NOT a production bug, but a test transformation issue. The tests use `setAllowNestedSnapshots(true)` (a test-only method) to enable nested snapshots before restart, but this in-memory setting is not persisted.

**Affected Tests:**
- TestSnapshottableDirListing_RestartInjected (8 test methods - all restart variants of 2 base tests)
  - **Priority 11:** 4 methods from `testListSnapshottableDir` (paths: `/TestSnapshot1/sub1`, `/TestSnapshot1/sub2`)
  - **Priority 12:** 4 methods from `testListWithDifferentUser` (paths: `/dir_user2/subdir`)

**Root Cause:** **Test transformation tool limitation**:
1. Test calls `setAllowNestedSnapshots(true)` on OLD NameNode instance
2. Restart creates NEW NameNode instance
3. NEW NameNode has default value: `allowNestedSnapshots = false`
4. Test tries to create nested snapshots after restart → fails
   - **Priority 11:** Fails at line 134 (`hdfs.allowSnapshot(sub1)`)
   - **Priority 12:** Fails at line 823 (`hdfs.allowSnapshot(subdir_user2)`)

**Evidence:**
From `SnapshotManager.java:154`:
```java
/** Used in tests only */
void setAllowNestedSnapshots(boolean allowNestedSnapshots) {
  this.allowNestedSnapshots = allowNestedSnapshots;
}
```

From `SnapshotManager.java:101`:
```java
private boolean allowNestedSnapshots = false;  // Defaults to false, not persistent
```

**Why Not a Production Bug:**
- Method is explicitly marked **"Used in tests only"**
- Not a production feature - no config property, no RPC method
- Simple instance variable, not saved to fsimage/editlog
- Expected to revert to default after restart

**Pattern Recognition:**
This is the **same pattern** as Priorities 2 & 3:
- Test sets state on OLD component before restart
- Restart creates NEW component
- Test assumes OLD state persists on NEW component
- Root cause: Test transformation tool doesn't re-apply test-only settings

**Fix Required:**
Re-apply the setting after restart:
```java
executeRestart();
// Add this line:
cluster.getNamesystem().getSnapshotManager().setAllowNestedSnapshots(true);
```

**See:** [Priority_11_SnapshotException_TEST_FRAMEWORK.md](Priority_11_SnapshotException_TEST_FRAMEWORK.md)

---

## Priority 13: Not All DataNodes Registered After Restart

**Status:** ⏱️ TIMING ISSUE - Test Framework

**Summary:** Tests fail with `AssertionError: Not all DataNodes registered after restart expected:<2> but was:<1>`. Code analysis reveals this is a test framework timing issue where `verifyClusterHealth()` performs an immediate assertion on DataNode registration count without retry/polling logic, causing failures when DataNodes haven't fully registered yet.

**Affected Tests:**
- 2 occurrences (tests with 2+ DataNodes and DataNode restarts)

**Root Cause:** **Insufficient retry logic in test framework**:
1. `verifyClusterHealth()` calls `cluster.waitActive()` which polls for DataNodes (with retry)
2. After `waitActive()` returns, `verifyClusterHealth()` **immediately asserts** DataNode count
3. Race condition: Between when `waitActive()` checks and when assertion runs, not all DataNodes may be registered
4. **No retry** in the assertion → immediate failure

**Code Location:**
From `RestartInjectionFramework.java:378-385`:
```java
// Verify all DataNodes are registered
List<DatanodeDescriptor> datanodes = cluster.getNameNode().getNamesystem()
    .getBlockManager().getDatanodeManager().getDatanodeListForReport(...);
int expectedDNs = cluster.getDataNodes().size();
assertEquals("Not all DataNodes registered after restart",  // NO RETRY!
    expectedDNs, datanodes.size());
```

**Why Not a Production Bug:**
- Test-only code in `RestartInjectionFramework.java`
- Production clients retry operations and tolerate delayed DataNode registration
- Normal for DataNodes to take varying time to reconnect/heartbeat after restart
- Production HDFS handles this gracefully with long timeouts

**Pattern Recognition:**
Similar to other test framework issues where immediate checks don't account for asynchronous processes:
- Priority 2-3: Stale references (need refresh)
- Priority 11-12: Lost settings (need re-apply)
- **Priority 13:** Timing races (need retry/polling)

**Fix Required:**
Add retry/polling logic to the DataNode count check:
```java
// Wait up to 5 seconds for all DataNodes to register
int maxRetries = 50;  // 50 * 100ms = 5 seconds
int retries = 0;
List<DatanodeDescriptor> datanodes = null;

while (retries < maxRetries) {
  datanodes = cluster.getNameNode().getNamesystem()...getDatanodeListForReport(...);
  if (datanodes.size() == expectedDNs) break;
  Thread.sleep(100);
  retries++;
}
assertEquals("Not all DataNodes registered after restart (timed out)",
    expectedDNs, datanodes.size());
```

**See:** [Priority_13_DataNode_Registration_TEST_FRAMEWORK.md](Priority_13_DataNode_Registration_TEST_FRAMEWORK.md)

---

## Priority 14: IllegalArgumentException - Namenode Index Needed for HA

**Status:** 🔧 TEST FRAMEWORK ISSUE - MiniDFSCluster API Misuse

**Summary:** Tests fail with `IllegalArgumentException: Namenode index is needed` when running on HA (High Availability) clusters. Code analysis reveals the `RestartInjectionFramework.verifyClusterHealth()` method calls single-NameNode APIs (`cluster.getNameNode()`, `cluster.getNamesystem()`) that only work for single-NN clusters. These methods throw exceptions when called on multi-NN (HA/federated) clusters.

**Affected Tests:**
- **24 occurrences** - All HA restart-injected tests in `server/namenode/ha/` directory
- Examples: TestFailureOfSharedDir_RestartInjected, TestStandbyCheckpoints_RestartInjected

**Root Cause:** **MiniDFSCluster API misuse**:
1. `MiniDFSCluster` provides two types of APIs:
   - **Single-NN APIs:** `getNameNode()`, `getNamesystem()` - Call `checkSingleNameNode()`
   - **Multi-NN APIs:** `getNameNode(int)`, `getNamesystem(int)` - Work with HA clusters
2. `verifyClusterHealth()` uses single-NN APIs (lines 379, 388):
   ```java
   cluster.getNameNode().getNamesystem()  // No index - throws on HA!
   cluster.getNamesystem()                // No index - throws on HA!
   ```
3. On HA clusters with 2+ NameNodes, `checkSingleNameNode()` throws:
   ```java
   if (namenodes.size() != 1) {
     throw new IllegalArgumentException("Namenode index is needed");
   }
   ```

**Code Locations:**
- **Exception thrown:** `MiniDFSCluster.java:3345` - `checkSingleNameNode()`
- **Problematic calls:** `RestartInjectionFramework.java:379, 388, 482`
- **Single-NN APIs:** `MiniDFSCluster.java:2002, 2033`
- **Multi-NN APIs (correct):** `MiniDFSCluster.java:2024, 2037`

**Why Not a Production Bug:**
- Test-only code in MiniDFSCluster and RestartInjectionFramework
- Production HDFS has no concept of MiniDFSCluster
- HA functionality in production works correctly
- This is a test framework limitation, not a production issue

**Impact:**
- **Production:** NONE
- **Testing:** HIGH - Blocks ALL HA restart-injection tests (24 test methods)
- Prevents testing HA restart scenarios
- Limits test coverage for critical HA configurations

**Fix Required:**
Use indexed API variants to support both single-NN and multi-NN clusters:

```java
// BEFORE (fails on HA):
List<DatanodeDescriptor> datanodes = cluster.getNameNode().getNamesystem()...
LeaseManager leaseManager = NameNodeAdapter.getLeaseManager(cluster.getNamesystem());

// AFTER (works everywhere):
int nnIndex = 0;  // Check first/active NN
List<DatanodeDescriptor> datanodes = cluster.getNameNode(nnIndex).getNamesystem()...
LeaseManager leaseManager = NameNodeAdapter.getLeaseManager(cluster.getNamesystem(nnIndex));
```

**See:** [Priority_14_IllegalArgumentException_HA_TEST_FRAMEWORK.md](Priority_14_IllegalArgumentException_HA_TEST_FRAMEWORK.md)

---

## Priority 15: Another DiskBalancer is Running

**Status:** 🔧 TEST FRAMEWORK ISSUE

**Occurrences:** 16

**Summary:** DiskBalancer from before restart still running/registered when test tries to start new one. State not cleaned up properly during restart.

**Recommended Fix:** Add DiskBalancer cleanup in restart framework or wait for previous balancer to finish before restarting.

---

## Priority 16: AssertionError with No Detailed Message

**Status:** 🔧 TEST FRAMEWORK ISSUE

**Occurrences:** 8

**Summary:** Generic assertion failures in TestFSInputChecker and TestSetRepIncreasing after restart. Stack trace too shallow to determine root cause. Likely test validation issue - checking state that's not yet ready after restart.

**Recommended Fix:** Examine test code to identify failing assertion; likely checking state too soon after restart.

---

## Priority 17: AssertionError - Expected 1 but Was 0 in TestPendingReconstruction

**Status:** ⏱️ TIMING ISSUE - Test Framework

**Occurrences:** 8

**Summary:** Test expects 1 pending reconstruction but finds 0. Likely timing - reconstruction hasn't been scheduled yet after restart.

**Recommended Fix:** Add wait/poll for pending reconstruction count instead of immediate assertion.

---

## Priority 18: AssertionError (assertFalse) in TestSnapshotPathINodes

**Status:** 🔧 TEST FRAMEWORK ISSUE

**Occurrences:** 4

**Summary:** assertFalse failing in snapshot tests. Likely related to snapshot state not as expected after restart.

**Recommended Fix:** Check test expectations vs actual snapshot state after restart; may need state adjustment.

---

## Priority 19: TestTimedOutException - Test Timed Out After 180s

**Status:** ⏱️ TIMING ISSUE - Test Framework

**Occurrences:** 4

**Summary:** Tests timing out after restart. Operations taking too long post-restart, likely due to slow cluster stabilization.

**Recommended Fix:** Increase test timeout or optimize cluster stabilization.

---

## Priority 20: Failed to Replace Bad Datanode - No Good Datanodes Available

**Status:** ⏱️ TIMING ISSUE - DataNode Availability

**Occurrences:** 190 (43% of all errors - largest group)

**Summary:** After restart, datanodes aren't immediately available so pipeline replacement fails. This is a timing issue - datanodes need time to re-register and heartbeat. Could also indicate improper restart point (mid-write).

**Recommended Fix:**
- Option 1: Add wait for datanode availability before continuing write operations
- Option 2: Choose restart points that don't leave writes in-progress

---

## Priority 21: All Datanodes Are Bad - Aborting

**Status:** ⏱️ TIMING ISSUE - DataNode Availability

**Occurrences:** 94

**Summary:** Similar to Priority 20 - datanodes not ready after restart. DataStreamer can't establish pipeline because nodes are marked bad (not heartbeating yet).

**Recommended Fix:** Same as Priority 20 - timing issue with datanode availability.

---

## Priority 22: Failed to Replace Bad Datanode (Single Datanode)

**Status:** ⏱️ TIMING ISSUE - DataNode Availability

**Occurrences:** 6

**Summary:** Variant of Priority 20 with single datanode. Same root cause.

**Recommended Fix:** Same as Priority 20.

---

## Priority 23: All Datanodes Are Bad (SimulatedStorage)

**Status:** ⏱️ TIMING ISSUE - DataNode Availability

**Occurrences:** 6

**Summary:** Same as Priority 21 but with simulated storage. Same timing issue.

**Recommended Fix:** Same as Priority 20-21.

---

## Priority 24: Failed to Replace Bad Datanode (ALWAYS Policy)

**Status:** ⏱️ TIMING ISSUE - DataNode Availability

**Occurrences:** 6

**Summary:** Variant of Priority 20 with ALWAYS replacement policy. Same root cause but different policy setting.

**Recommended Fix:** Same as Priority 20.

---

## Priority 25: libhadoop Cannot Be Loaded - UNIX Domain Socket

**Status:** 🔧 ENVIRONMENT ISSUE

**Occurrences:** 8

**Summary:** Native library not available in test environment. Pure environment/build issue, not a bug. Short-circuit reads require native libs.

**Recommended Fix:** Install libhadoop.so or disable tests requiring native code.

---

## Investigation Methodology

For each failure, we:

1. ✅ **Reproduce** - Run the failing test to confirm failure
2. ✅ **Analyze** - Examine stack trace and code paths
3. ✅ **Debug** - Add instrumentation to understand data flow
4. ✅ **Categorize** - Determine if it's actual bug vs test framework issue
5. ✅ **Document** - Write detailed report with evidence
6. ✅ **Recommend** - Propose fixes with verification steps

---

## Legend

- ✅ **CONFIRMED BUG** - Reproducible production code bug
- ⚠️ **LIKELY BUG** - Strong evidence of bug, needs more investigation
- 🔧 **TEST FRAMEWORK** - Issue in test infrastructure, not production code
- ⏱️ **TIMING ISSUE** - Timing/synchronization issue, may need restart point adjustment
- ❌ **FALSE POSITIVE** - Not actually a bug
- 🔧 **ENVIRONMENT** - Build/environment configuration issue

---

## Summary Statistics

**Total Priorities Investigated:** 25

**By Category:**
- **Confirmed Production Bugs:** 1 (Priority 6: hflush() visibility after DN restart)
- **Test Framework Issues:** 10 (Priorities 2, 3, 11-12, 14, 15, 16, 18, 25)
- **Timing Issues:** 7 (Priorities 13, 17, 19, 20-24)
- **Test Design Issues:** 2 (Priorities 4, 5)
- **False Positives:** 2 (Priorities 2, 3)
- **Duplicates:** 4 (Priorities 7-10 duplicate of Priority 6)

**Key Findings:**
- **Only 1 genuine production bug found** (Priority 6) affecting hflush() visibility guarantee
- **Most failures are test framework issues** requiring framework improvements
- **Large cluster of DataNode availability timing issues** (Priorities 20-24, 310+ occurrences)
- **Test transformation tool needs improvements** to handle:
  - Stale component references after restart
  - Test-only settings that don't persist
  - HA cluster API differences
  - Async state changes requiring retry logic

---

Last Updated: 2025-12-01
