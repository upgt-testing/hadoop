# Test Analysis Summary

This summary shows the analysis results for the test logs in `restart-test-log-20251130_220042/`.

## Overall Test Statistics

- **Total Test Classes**: 444
- **Total Test Methods**: 3,759
- **Passed**: 3,295 (87.7%)
- **Failed**: 26 (0.7%)
- **Errors**: 438 (11.7%)
- **Skipped**: 0 (0.0%)

## Grouped Root Cause Analysis

### Failures
- **Total Failures**: 26 test methods
- **Unique Root Causes**: 5 distinct failure signatures

The largest failure groups:
1. **8 tests** - AssertionError in assertTrue/fail (TestFSInputChecker, TestSetRepIncreasing)
2. **8 tests** - AssertionError expected:<1> but was:<0> (TestPendingReconstruction)
3. **4 tests** - AssertionError in BlockManager.findAndMarkBlockAsCorrupt
4. **4 tests** - AssertionError in assertFalse (TestSnapshotPathINodes)
5. **2 tests** - AssertionError in verifyClusterHealth (TestBlocksScheduledCounter)

### Errors
- **Total Errors**: 438 test methods
- **Unique Root Causes**: 36 distinct error signatures

The largest error group:
1. **190 tests** - IOException: Failed to replace a bad datanode on the existing pipeline
   - This single issue affects 190 test methods across multiple test classes
   - Indicates a common problem with datanode replacement during restart tests

Other significant error groups:
- **48 tests** - RemoteException: AlreadyBeingCreatedException
- **32 tests** - RuntimeException: Cannot load libhadoop (UNIX domain socket)
- **16 tests** - RemoteException: RecoveryInProgressException
- **16 tests** - IOException: All datanodes are bad (connectivity/availability issues)

## Key Insights

1. **Single Dominant Error**: The "Failed to replace a bad datanode" error accounts for 43% of all errors (190/438)
2. **High Success Rate**: Despite errors, 87.7% of test methods pass
3. **Focused Failure Patterns**: Only 5 unique failure patterns, suggesting specific issues
4. **Restart-Related Issues**: Most errors relate to datanode replacement and recovery, which is expected for restart testing

## Files Generated

- `detailed_test_report.txt` - Complete per-test-class breakdown
- `grouped_failures_errors_report.txt` - Failures/errors grouped by root cause

## Scripts Used

1. `analyze_test_results.py` - Generated overall statistics
2. `group_failures_by_stacktrace.py` - Performed root cause grouping
