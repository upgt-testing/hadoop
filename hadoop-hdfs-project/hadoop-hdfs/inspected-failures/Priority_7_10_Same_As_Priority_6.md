# Priorities 7-10: CannotObtainBlockLengthException - Same as Priority 6

**Status:** ✅ CONFIRMED - SAME BUG AS PRIORITY 6

**See:** [Priority_6_CONFIRMED_BUG.md](Priority_6_CONFIRMED_BUG.md) for full investigation

---

## Summary

Priorities 7-10 in the failure ranking are **the same bug** as Priority 6, just affecting different test files. All are caused by the same root issue: `ReplicaWaitingToBeRecovered.getVisibleLength()` returning -1 after DataNode restart.

---

## Affected Priorities and Test Files

| Priority | File Path | Test Class | Occurrences |
|----------|-----------|------------|-------------|
| 6 | `/tmp/abc.log` | TestBlockToken_RestartInjected | 6 |
| 7 | `/user/shuai/dataprotocol.dat` | TestClientProtocolForPipelineRecovery_RestartInjected | 6 |
| 8 | `/unfinished-block-buffer` | Various | 6 |
| 9 | `/unfinished-block` | Various | 6 |
| 10 | `/foo` | Various | 5 |

**Total affected test methods:** ~29 across all priorities

---

## Confirmation

### Priority 7 Test Run

**Test:** `TestClientProtocolForPipelineRecovery_RestartInjected.testGetNewStamp_AfterHflush_NNDN_Crash`

**Error:**
```
org.apache.hadoop.hdfs.CannotObtainBlockLengthException: Cannot obtain block length
for LocatedBlock{BP-***:blk_1073741825_1002; getBlockSize()=2; ...}
of /user/shuai/dataprotocol.dat
```

**Stack Trace:** Identical to Priority 6
- `DFSInputStream.readBlockLength()` at line 414
- `DFSInputStream.getLastBlockLength()` at line 323
- Cannot get visible length from DataNode after restart

---

## Root Cause (Same as Priority 6)

All these failures have the **exact same root cause**:

1. Test writes data and calls `hflush()` → creates under-construction block
2. **DataNode restarts** (or NameNode+DataNode restart)
3. DataNode loads UC block into `ReplicaWaitingToBeRecovered` state
4. RWR state returns **visible length = -1**
5. Client tries to open file for reading
6. Gets -1 from DataNode → throws `CannotObtainBlockLengthException`

---

## Why Different File Paths?

The different file paths are simply because different test classes use different file naming conventions:

- **TestBlockToken_RestartInjected:** Uses `/tmp/abc.log`
- **TestClientProtocolForPipelineRecovery_RestartInjected:** Uses `/user/shuai/dataprotocol.dat`
- Other tests use `/foo`, `/unfinished-block`, etc.

The file path is **irrelevant** to the bug - it's the state of the under-construction block after DataNode restart that causes the issue.

---

## Impact

Same as Priority 6:
- ✅ CONFIRMED PRODUCTION BUG
- **Severity:** HIGH
- Breaks HDFS's `hflush()` visibility guarantee after DataNode restart
- Affects HBase WALs, streaming applications, and any app reading UC files

---

## Recommended Action

**Do not investigate separately.** These are all manifestations of the same bug documented in Priority 6.

**Fix:** See Priority 6 report for recommended fixes:
1. Modify `ReplicaWaitingToBeRecovered.getVisibleLength()` to return actual bytes on disk
2. OR trigger automatic lease recovery when visible length = -1
3. OR improve replica state detection on DataNode restart

Once Priority 6 is fixed, **all of Priorities 7-10 will be resolved**.

---

## Verification

All affected tests can be verified with the same approach as Priority 6:

```bash
# Run any of the failing tests
mvn surefire:test -Dtest=TestClientProtocolForPipelineRecovery_RestartInjected#testGetNewStamp_AfterHflush_NNDN_Crash

# Currently fails with CannotObtainBlockLengthException
# After fix: should pass
```

---

**Conclusion:** Priorities 7-10 are **NOT separate bugs**. They are the same bug as Priority 6, just discovered in different test files. Consolidate all investigation and fixing efforts under Priority 6.

---

*Report Date: 2025-12-01*
*Status: Confirmed duplicate of Priority 6*
