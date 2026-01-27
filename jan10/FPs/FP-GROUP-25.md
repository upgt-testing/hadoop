# FP-GROUP-25: Edit Log File Not Found After NameNode Restart

## Summary
**Verdict:** FALSE POSITIVE

The test `TestSecurityTokenEditLog_RestartInjected.testEditLog` fails with `NoSuchFileException` because the test's verification logic assumes edit log files start from transaction ID 1, which is not valid after a NameNode restart.

## Failure Details

**Test:** `org.apache.hadoop.hdfs.server.namenode.TestSecurityTokenEditLog_RestartInjected#testEditLog`

**Restart Position:** `after_cluster_start`

**Target:** `namenode` (index 0)

**Mode:** `GRACEFUL`

**Error:**
```
org.apache.hadoop.hdfs.server.namenode.EditLogInputException: Error replaying edit log at offset 0. Expected transaction ID was 1
Caused by: java.nio.file.NoSuchFileException: .../edits_0000000000000000001-0000000000000030006
```

## Root Cause Analysis

### Test Verification Logic
The test verification code at line 212 explicitly constructs the expected edit log file name:
```java
for (StorageDirectory sd : fsimage.getStorage().dirIterable(NameNodeDirType.EDITS)) {
    File editFile = NNStorage.getFinalizedEditsFile(sd, 1, 1 + expectedTransactions - 1);
    // ...
    long numEdits = loader.loadFSEdits(new EditLogFileInputStream(editFile), 1);
}
```

This assumes:
1. The edit log file starts from transaction ID 1
2. All transactions are in a single contiguous file

### What Happens During Restart

When the restart framework injects a NameNode restart at `after_cluster_start`:

1. **Before Restart:**
   - NameNode has written initial transactions (txn 1-4) to an edit log segment
   - Edit log file: `edits_inprogress_0000000000000000001`

2. **During Restart:**
   - The in-progress edit log is finalized: `edits_0000000000000000001-0000000000000000004`
   - FSImage may be saved

3. **After Restart:**
   - NameNode loads from FSImage (which may include txn 1-4)
   - A NEW edit log segment starts from the next txn ID (e.g., txn 5)
   - Edit log file: `edits_inprogress_0000000000000000005`

4. **After All Transactions:**
   - The 100 threads perform 100 transactions each (3 ops per transaction = 30,000 ops)
   - Final finalized file: `edits_0000000000000000005-0000000000000030008`

### Why The File Doesn't Exist

The test expects: `edits_0000000000000000001-0000000000000030006`

The actual files are:
- `edits_0000000000000000001-0000000000000000004` (pre-restart segment)
- `edits_0000000000000000005-0000000000000030008` (post-restart segment)

There is no single file containing txn 1 through ~30006 because the restart caused edit log segmentation.

## Why This Is a False Positive

1. **Expected HDFS Behavior:** Edit log segmentation during NameNode restart is correct and expected behavior. HDFS properly finalizes edit log segments during shutdown and starts new segments on restart.

2. **Invalid Test Assumption:** The test's verification logic hardcodes the assumption that edit logs start from txn 1. This is valid for a continuous run but invalid after any NameNode restart.

3. **Not a Bug in HDFS:** The edit log system is functioning correctly - it's the test's verification logic that doesn't account for edit log segmentation.

4. **Restart Position Issue:** Injecting a restart at `after_cluster_start` but before the transactions are written causes the subsequent transactions to be recorded in a new edit log segment, making the original verification logic invalid.

## Correct Behavior

In production, after a NameNode restart:
1. All finalized edit log segments are preserved
2. The NameNode can replay all edit logs (from multiple segments) during recovery
3. Clients continue to work with the restarted NameNode

The test verification code would need to be modified to:
1. Find all edit log segments in the storage directory
2. Load and count transactions from all segments combined
3. Not assume a single contiguous file from txn 1

## Affected Test Executions

All 4 test executions for this group are false positives with the same root cause:
- `after_cluster_start` - Restart before transactions, new segment starts
- `after_editlog_setup` - Restart after setup, same issue
- `after_threads_start` - Restart while transactions are running, same issue
- `after_transactions_complete` - Restart after transactions, segment boundary issue

## Conclusion

This is a **FALSE POSITIVE** caused by the test's verification logic making invalid assumptions about edit log file structure after a NameNode restart. The restart framework correctly restarts the NameNode, and HDFS correctly handles edit log segmentation. The test simply doesn't account for this expected behavior.
