# YARN Restart Test Analysis Scripts

## Overview

This directory contains Python scripts to analyze YARN restart test results, extract all test method results, gather failures, and group them by normalized stack traces.

## Scripts

### 1. `analyze_restart_tests.py`

Main analysis script that:
- Parses all JSON test reports from restart test output directories
- Extracts all test method results (baseline + restart scenarios)
- Gathers all failures/errors
- Groups failures by normalized stack trace (removes line numbers and truncates after test method name)
- Generates comprehensive summary reports

**Usage:**
```bash
# Analyze the most recent test output
python3 analyze_restart_tests.py

# Analyze a specific test output directory
python3 analyze_restart_tests.py restart-test-output-20251203-002527
```

**Output:**
- Console summary with module breakdown and failure groups
- `ANALYSIS.json` - Detailed JSON analysis with all test results

### 2. `generate_failure_report.py`

Report generator that creates multiple report formats from the analysis:
- **Markdown report** (`FAILURE_REPORT.md`) - Human-readable summary
- **HTML report** (`FAILURE_REPORT.html`) - Interactive web view
- **CSV report** (`FAILURES.csv`) - Spreadsheet-compatible (only if failures exist)

**Usage:**
```bash
# Generate reports from the most recent analysis
python3 generate_failure_report.py

# Generate reports from a specific analysis file
python3 generate_failure_report.py restart-test-output-20251203-002527/ANALYSIS.json
```

**Output Files:**
- `FAILURE_REPORT.md` - Markdown format report
- `FAILURE_REPORT.html` - HTML format report (open in browser)
- `FAILURES.csv` - CSV format (only created if failures exist)

## Stack Trace Normalization

The scripts normalize stack traces to group similar failures together:

### Normalization Rules:

1. **Remove all line numbers**
   - Before: `at org.example.Test.method(Test.java:123)`
   - After: `at org.example.Test.method(Test.java)`

2. **Truncate after test method name appears**
   - Only keeps the stack trace up to and including the test method
   - Removes framework-level calls (JUnit, Maven, etc.)

### Example:

**Original Stack Trace:**
```
java.io.IOException: Connection timeout
    at org.apache.hadoop.yarn.Client.connect(Client.java:445)
    at org.apache.hadoop.yarn.Client.submit(Client.java:123)
    at org.apache.hadoop.yarn.TestYarnClient.testSubmit(TestYarnClient.java:89)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
```

**Normalized Stack Trace:**
```
java.io.IOException: Connection timeout
    at org.apache.hadoop.yarn.Client.connect(Client.java)
    at org.apache.hadoop.yarn.Client.submit(Client.java)
    at org.apache.hadoop.yarn.TestYarnClient.testSubmit(TestYarnClient.java)
```

This allows grouping failures that have the same root cause but fail at different line numbers or in different test scenarios.

## Complete Workflow

### Run Tests and Analyze

```bash
# 1. Run all restart tests
./run-all-restart-tests.sh

# 2. Analyze the results
python3 analyze_restart_tests.py

# 3. Generate detailed reports
python3 generate_failure_report.py
```

### View Reports

```bash
# View Markdown report in terminal
cat restart-test-output-*/FAILURE_REPORT.md

# Open HTML report in browser
open restart-test-output-*/FAILURE_REPORT.html

# View JSON analysis
cat restart-test-output-*/ANALYSIS.json | jq .
```

## Analysis JSON Structure

The `ANALYSIS.json` file contains:

```json
{
  "summary": {
    "totalExecutions": 530,
    "passedExecutions": 530,
    "failedExecutions": 0,
    "passRate": 100.0,
    "uniqueTestMethods": 110,
    "totalDurationMs": 15037890,
    "totalDurationMinutes": 250.63,
    "modulesAnalyzed": 6,
    "uniqueFailurePatterns": 0
  },
  "moduleSummaries": {
    "hadoop-yarn-client": {
      "totalTests": 295,
      "passed": 295,
      "failed": 0,
      "passRate": 100.0
    }
  },
  "failureGroups": [
    {
      "exceptionType": "java.io.IOException",
      "normalizedStackTrace": "...",
      "failureCount": 5,
      "affectedTestMethods": ["TestYarnClient.testSubmit"],
      "affectedModules": ["hadoop-yarn-client"],
      "failures": [...]
    }
  ],
  "allFailures": [...],
  "allResults": [...]
}
```

## Understanding Test Results

### Test Execution Types

Each test method generates multiple test executions:

1. **Baseline execution** - No restart (position=null, target=null, mode=null)
2. **Restart scenarios** - Multiple combinations of:
   - **Position**: Where in the test to trigger restart (e.g., "after_cluster_start")
   - **Target**: Which node to restart (e.g., "resourcemanager", "nodemanager")
   - **Mode**: How to restart (e.g., "GRACEFUL", "CRASH", "DELAYED_CRASH")

### Example Test Matrix

For a test with 2 positions × 2 targets × 2 modes:
```
TestYarnClient.testSubmit
├── BASELINE (no restart)
├── Position: after_cluster_start
│   ├── Target: resourcemanager, Mode: GRACEFUL
│   ├── Target: resourcemanager, Mode: CRASH
│   ├── Target: nodemanager, Mode: GRACEFUL
│   └── Target: nodemanager, Mode: CRASH
└── Position: before_app_submit
    ├── Target: resourcemanager, Mode: GRACEFUL
    ├── Target: resourcemanager, Mode: CRASH
    ├── Target: nodemanager, Mode: GRACEFUL
    └── Target: nodemanager, Mode: CRASH
```
Total: 9 executions (1 baseline + 8 restart scenarios)

## Current Test Run Results

From `restart-test-output-20251203-002527`:

- **Total Modules**: 6
- **Unique Test Methods**: 110
- **Total Executions**: 530
- **Pass Rate**: 100% ✓
- **Duration**: 250.63 minutes

### Module Breakdown

| Module | Tests | Status |
|--------|-------|--------|
| hadoop-yarn-client | 295 | ✓ All passed |
| hadoop-yarn-applications-distributedshell | 116 | ✓ All passed |
| hadoop-yarn-services-core | 68 | ✓ All passed |
| hadoop-yarn-server-tests | 37 | ✓ All passed |
| hadoop-yarn-applications-unmanaged-am-launcher | 8 | ✓ All passed |
| hadoop-yarn-services-api | 6 | ✓ All passed |

## Interpreting Failure Groups

When failures occur, they are grouped by normalized stack trace:

### Failure Group Information

Each group shows:
- **Exception Type**: The type of exception (e.g., IOException, NullPointerException)
- **Failure Count**: Number of test executions that failed with this pattern
- **Affected Test Methods**: Which test methods experienced this failure
- **Affected Modules**: Which modules had this failure
- **Normalized Stack Trace**: The normalized stack trace pattern
- **Failure Scenarios**: Specific restart configurations that failed

### Example Failure Group

```markdown
### Group #1: java.io.IOException

- **Failure Count**: 5
- **Affected Test Methods**: 2
- **Affected Modules**: hadoop-yarn-client

#### Affected Test Methods
- `org.apache.hadoop.yarn.client.api.impl.TestYarnClient.testSubmit`
- `org.apache.hadoop.yarn.client.api.impl.TestYarnClient.testResubmit`

#### Failure Scenarios
| Test Method | Position | Target | Mode | Module |
|-------------|----------|--------|------|--------|
| testSubmit | after_cluster_start | resourcemanager | CRASH | hadoop-yarn-client |
| testSubmit | before_app_submit | resourcemanager | CRASH | hadoop-yarn-client |
| testResubmit | after_failure | resourcemanager | CRASH | hadoop-yarn-client |
```

This indicates that these tests fail specifically when the ResourceManager crashes (not graceful shutdown), suggesting a potential issue with crash recovery logic.

## Tips

### Finding Patterns in Failures

1. **By Exception Type**: Look at which exceptions are most common
2. **By Module**: Identify which modules have the most failures
3. **By Test Method**: Find which tests are most fragile
4. **By Restart Configuration**: Identify problematic scenarios (e.g., all CRASH mode failures)

### Debugging Failed Tests

1. Look at the **normalized stack trace** to understand the root cause
2. Check the **failure scenarios** to see if there's a pattern (e.g., only fails with CRASH mode)
3. Review the **full stack trace** in the HTML report for complete details
4. Check if the same test passes in baseline but fails with restarts (indicates restart-related issue)

### Performance Analysis

Even when all tests pass, the analysis shows:
- Total duration by module
- Individual test execution times
- Which tests are slowest

Use this to identify performance bottlenecks.

## Requirements

- Python 3.6+
- No external dependencies (uses only standard library)

## Files Generated

After running the scripts, you'll have:

```
restart-test-output-20251203-002527/
├── SUMMARY.txt                    # Original test run summary
├── ANALYSIS.json                  # Detailed analysis (323KB)
├── FAILURE_REPORT.md             # Markdown report
├── FAILURE_REPORT.html           # HTML report (view in browser)
├── FAILURES.csv                  # CSV export (only if failures exist)
└── [module directories]/
    ├── restart-test-report.json
    ├── restart-test-report.html
    └── test-execution.log
```

## Next Steps

1. **Investigate Failures**: Use the failure groups to identify common patterns
2. **Fix Issues**: Address the root causes identified in the normalized stack traces
3. **Re-run Tests**: Execute `./run-all-restart-tests.sh` to verify fixes
4. **Track Progress**: Compare ANALYSIS.json files across test runs to track improvements
