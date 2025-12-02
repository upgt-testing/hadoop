# Test Analysis Scripts

This directory contains scripts for analyzing Maven Surefire test logs.

## Scripts

1. **analyze_test_results.py** - Generates test statistics and summaries
2. **group_failures_by_stacktrace.py** - Groups failures/errors by their root cause

---

## analyze_test_results.py

A Python script that parses Maven Surefire test execution logs and generates comprehensive statistics about test results.

### Features

- Parses Maven Surefire log files to extract test execution results
- Handles ANSI color codes in log files
- Aggregates test statistics across all test classes
- Identifies and reports:
  - Passed tests
  - Failed tests
  - Tests with errors
  - Skipped tests
- Generates detailed reports per test class

### Usage

Basic usage:
```bash
python3 scripts/analyze_test_results.py <log_directory>
```

Generate detailed report:
```bash
python3 scripts/analyze_test_results.py <log_directory> --detailed
```

### Example

```bash
# Analyze logs in the restart-test-log-20251130_220042 directory
python3 scripts/analyze_test_results.py restart-test-log-20251130_220042/

# Generate a detailed report file
python3 scripts/analyze_test_results.py restart-test-log-20251130_220042/ --detailed
```

### Output

The script outputs:
1. **Console Summary**:
   - Total test classes and methods
   - Pass/Fail/Error/Skip statistics with percentages
   - List of failed test classes
   - List of test classes with errors
   - List of test classes with skipped tests

2. **Detailed Report** (with --detailed flag):
   - All information from console summary
   - Complete listing of all test classes with their individual statistics
   - Saved to `<log_directory>/detailed_test_report.txt`

### Log File Format

The script expects Maven Surefire log files with the following format:
```
[INFO] Tests run: X, Failures: Y, Errors: Z, Skipped: W
```

The script automatically:
- Removes ANSI color codes from log files
- Looks for the final test summary in the "Results:" section
- Falls back to parsing the test execution line if needed

### Requirements

- Python 3.6 or higher
- No external dependencies (uses only standard library)

---

## group_failures_by_stacktrace.py

A Python script that analyzes test failures and errors, grouping them by their root cause based on normalized stack traces. This helps identify common failure patterns across multiple tests.

### Features

- Extracts stack traces from test failures and errors
- Normalizes stack traces by:
  - Removing line numbers (e.g., `File.java:123` → `File.java`)
  - Removing variable data (IDs, timestamps, IP addresses, ports, etc.)
  - Truncating at the test method frame (removes test-specific frames)
- Groups tests that fail with the same root cause
- Identifies unique failure/error signatures
- Shows which tests are affected by each issue

### Usage

Basic usage:
```bash
python3 scripts/group_failures_by_stacktrace.py <log_directory>
```

Save detailed report to file:
```bash
python3 scripts/group_failures_by_stacktrace.py <log_directory> --save
```

### Example

```bash
# Analyze and group failures/errors
python3 scripts/group_failures_by_stacktrace.py restart-test-log-20251130_220042/

# Generate a detailed report file
python3 scripts/group_failures_by_stacktrace.py restart-test-log-20251130_220042/ --save
```

### Output

The script outputs:

1. **Console Summary**:
   - Total failures and unique failure signatures
   - Total errors and unique error signatures
   - For each failure/error group:
     - Number of occurrences
     - List of affected test classes and methods
     - Normalized stack trace showing the root cause

2. **Detailed Report** (with --save flag):
   - Complete information from console summary
   - Full normalized stack traces for each group
   - Saved to `<log_directory>/grouped_failures_errors_report.txt`

### Stack Trace Normalization

The script normalizes stack traces to group similar failures together by:

1. **Removing Line Numbers**:
   - `(DFSInputStream.java:414)` → `(DFSInputStream.java)`

2. **Removing Variable Data**:
   - Block pool IDs: `BP-1772494331-127.0.1.1-1764570987883` → `BP-***`
   - Block IDs: `blk_1073741825_1001` → `blk_***`
   - IP addresses/ports: `127.0.0.1:43191` → `***.***.***:***`
   - UUIDs: `DS-b51f4220-85f2-4f7f-9a0f-7aa9c17f8f54` → `DS-***`
   - Timestamps and large IDs: `1764570987883` → `***`

3. **Truncating at Test Frame**:
   - Removes frames from the test method onwards
   - Example: Stops at `TestBlockRecovery2_RestartInjected.testMethod(...)`
   - Keeps only the application/framework stack trace

This normalization ensures that tests failing with the same root cause are grouped together, even if they have different:
- Test method names
- Line numbers where the failure occurred
- Runtime-specific identifiers

### Use Cases

- **Identify Common Failures**: See which errors affect the most tests
- **Root Cause Analysis**: Focus on unique failure patterns instead of individual test failures
- **Prioritize Fixes**: Address the most impactful issues first (those affecting many tests)
- **Track Progress**: Re-run after fixes to see if failure groups are resolved

### Requirements

- Python 3.6 or higher
- No external dependencies (uses only standard library)
