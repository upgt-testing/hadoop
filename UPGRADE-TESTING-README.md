# Hadoop 3.3.5 → 3.3.6 Upgrade Testing Guide

This guide provides complete instructions for testing Hadoop upgrades from version 3.3.5 to 3.3.6 using the ProcessBasedMiniDFSCluster framework.

> **📝 Note:** Tests now use **system properties** (`-Dhadoop.start.home` and `-Dhadoop.upgrade.home`) instead of environment variables for easier execution via Maven. The cluster automatically reads these properties - no manual setup needed in test code!

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Prerequisites](#prerequisites)
4. [Scripts and Tools](#scripts-and-tools)
5. [Running Tests](#running-tests)
6. [Test Scenarios](#test-scenarios)
7. [Analyzing Results](#analyzing-results)
8. [Troubleshooting](#troubleshooting)
9. [Advanced Usage](#advanced-usage)

## Overview

This testing infrastructure provides comprehensive validation of Hadoop version upgrades including:

- **Rolling upgrades** - Upgrade DataNodes one-by-one without downtime
- **Mixed-version clusters** - Verify different versions can coexist
- **Data integrity** - Ensure no data loss or corruption during upgrades
- **Performance testing** - Measure upgrade impact under load
- **Downgrade scenarios** - Verify rollback procedures

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  run-upgrade-test.sh                                        │
│  • Downloads Hadoop 3.3.5 & 3.3.6 distributions            │
│  • Sets up environment variables                            │
│  • Builds Hadoop project                                    │
│  • Executes upgrade tests                                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  ProcessBasedMiniDFSCluster Framework                       │
│  • Spawns NameNode & DataNode processes                    │
│  • Supports multiple Hadoop versions                        │
│  • Enables process-level isolation                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  Test Classes                                               │
│  • TestHadoop335To336Upgrade (7 comprehensive tests)        │
│  • TestRollingUpgrade (9 upgrade scenarios)                 │
│  • TestMixedVersionCluster (8 compatibility tests)          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│  analyze-test-results.sh                                    │
│  • Parses test results                                      │
│  • Generates performance metrics                            │
│  • Provides recommendations                                 │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

The fastest way to run upgrade tests:

```bash
# 1. Run the automated test script (downloads distributions, builds, and tests)
./run-upgrade-test.sh

# 2. Analyze the results
./analyze-test-results.sh
```

That's it! The script will:
- Download Hadoop 3.3.5 and 3.3.6 (~700MB total)
- Build the Hadoop project
- Run comprehensive upgrade tests
- Generate detailed reports

Expected runtime: 20-40 minutes (depending on network speed and system performance)

## Prerequisites

### System Requirements

- **OS**: Linux or macOS
- **Java**: JDK 8
- **Maven**: 3.3 or later
- **Memory**: At least 4GB RAM available
- **Disk**: At least 10GB free space
- **Network**: Internet access for downloading Hadoop distributions

### Required Tools

```bash
# Verify Java version
java -version  # Should show 1.8.x

# Verify Maven version
mvn -version   # Should show 3.3+

# Required utilities
which wget     # For downloading distributions
which tar      # For extracting archives
which bc       # For calculations in analysis script
```

### Environment Setup

No manual environment setup is required! The `run-upgrade-test.sh` script automatically:
- Downloads required Hadoop distributions
- Passes `hadoop.start.home` and `hadoop.upgrade.home` system properties
- Configures the test environment

**Note:** Tests now use system properties instead of environment variables for easier execution.

## Scripts and Tools

### 1. run-upgrade-test.sh

Main test execution script with comprehensive automation.

**Usage:**
```bash
./run-upgrade-test.sh [OPTIONS]
```

**Options:**
- `--skip-download` - Skip downloading distributions (use existing)
- `--skip-build` - Skip building Hadoop project
- `--test-class NAME` - Run specific test class only
- `--test-method NAME` - Run specific test method only
- `--clean` - Clean up downloaded distributions and build artifacts
- `--help` - Show help message

**Examples:**
```bash
# Run all tests (full automation)
./run-upgrade-test.sh

# Run tests, skip download (if distributions already exist)
./run-upgrade-test.sh --skip-download

# Run specific test class
./run-upgrade-test.sh --test-class TestHadoop335To336Upgrade

# Run specific test method
./run-upgrade-test.sh \
  --test-class TestHadoop335To336Upgrade \
  --test-method testBasicRollingUpgrade335To336

# Clean up everything
./run-upgrade-test.sh --clean
```

### 2. analyze-test-results.sh

Analyzes test results and generates detailed reports.

**Usage:**
```bash
./analyze-test-results.sh [TEST_RESULTS_DIR]
```

If no directory is specified, it analyzes the most recent test run.

**Output includes:**
- Test execution summary (pass/fail counts)
- Success rate percentage
- Test execution times
- Failure analysis with common error patterns
- Upgrade performance metrics
- Actionable recommendations

**Example:**
```bash
# Analyze most recent test results
./analyze-test-results.sh

# Analyze specific test run
./analyze-test-results.sh test-results-20250120-143022
```

## Running Tests

### Option 1: Run All Tests (Recommended)

```bash
./run-upgrade-test.sh
```

This runs the complete test suite including:
- 7 tests from `TestHadoop335To336Upgrade`
- 9 tests from `TestRollingUpgrade`
- 8 tests from `TestMixedVersionCluster`

Total: ~24 integration tests

### Option 2: Run Specific Test Suite

```bash
# Run only 3.3.5 → 3.3.6 specific tests
./run-upgrade-test.sh --test-class TestHadoop335To336Upgrade

# Run all rolling upgrade tests
./run-upgrade-test.sh --test-class TestRollingUpgrade

# Run mixed version cluster tests
./run-upgrade-test.sh --test-class TestMixedVersionCluster
```

### Option 3: Run Individual Test

```bash
# Test basic rolling upgrade
./run-upgrade-test.sh \
  --test-class TestHadoop335To336Upgrade \
  --test-method testBasicRollingUpgrade335To336

# Test upgrade under load
./run-upgrade-test.sh \
  --test-class TestHadoop335To336Upgrade \
  --test-method testUpgradeUnderLoad
```

### Option 4: Manual Execution

If you prefer manual control:

```bash
# 1. Download distributions manually
mkdir -p /tmp/hadoop-test-distributions
cd /tmp/hadoop-test-distributions
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.5/hadoop-3.3.5.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
tar xzf hadoop-3.3.5.tar.gz
tar xzf hadoop-3.3.6.tar.gz

# 2. Build Hadoop project
cd /path/to/hadoop-transform
mvn install -pl hadoop-hdfs-project/hadoop-hdfs -am -DskipTests

# 3. Run tests with system properties
mvn test \
  -pl hadoop-hdfs-project/hadoop-hdfs \
  -Dtest=TestHadoop335To336Upgrade \
  -Dhadoop.start.home=/tmp/hadoop-test-distributions/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/tmp/hadoop-test-distributions/hadoop-3.3.6
```

**Note:** System properties are now preferred over environment variables. The cluster automatically reads `hadoop.start.home` and `hadoop.upgrade.home` from system properties.

## Test Scenarios

### TestHadoop335To336Upgrade

Dedicated test suite for 3.3.5 → 3.3.6 upgrades:

| Test | Description | Duration |
|------|-------------|----------|
| `testBasicRollingUpgrade335To336` | Standard rolling upgrade with data verification | ~3-5 min |
| `testRollingDowngrade336To335` | Rollback from 3.3.6 to 3.3.5 | ~3-5 min |
| `testMixedVersionCluster335And336` | Mixed 3.3.5/3.3.6 cluster operation | ~2-3 min |
| `testUpgradeUnderLoad` | Upgrade with concurrent writes | ~5-8 min |
| `testLargeDatasetUpgrade` | Upgrade with 500MB dataset | ~8-12 min |
| `testPartialUpgrade` | Partial upgrade scenario | ~5-7 min |
| `testVersionCompatibility335And336` | Version compatibility checks | ~1 sec |

### TestRollingUpgrade

General rolling upgrade scenarios:

- Basic rolling upgrade
- Rolling upgrade with continuous reads
- Rolling upgrade with writes
- Reverse order upgrade
- Partial rolling upgrade
- Node failure during upgrade
- Rolling downgrade
- Multiple successive upgrades
- Large dataset upgrade

### TestMixedVersionCluster

Version compatibility testing:

- Same version cluster (baseline)
- Mixed patch versions
- Mixed minor versions
- Version compatibility checks
- DataNode restart with version change
- All DataNodes on different versions
- Incompatible version detection
- NameNode restart
- Mixed-version cluster stability

## Analyzing Results

### Test Output Structure

After running tests, you'll find:

```
test-results-YYYYMMDD-HHMMSS/
├── test-execution.log          # Full Maven test output
├── surefire-reports/           # JUnit XML reports
│   ├── TEST-*.xml              # Individual test results
│   └── *.txt                   # Test output logs
└── SUMMARY.txt                 # Quick summary report
```

### Understanding the Summary

The `SUMMARY.txt` file shows:

```
=======================================================================
Hadoop Upgrade Test Summary
=======================================================================

Test Execution Date: Thu Jan 20 14:30:22 PST 2025
Source Version:      Hadoop 3.3.5
Target Version:      Hadoop 3.3.6

Environment:
  HADOOP_3_3_5_HOME: /tmp/hadoop-test-distributions/hadoop-3.3.5
  HADOOP_3_3_6_HOME: /tmp/hadoop-test-distributions/hadoop-3.3.6

Test Results:
  Tests run:     24
  Failures:      0
  Errors:        0
  Skipped:       0

STATUS: SUCCESS ✓
```

### Using the Analysis Script

Run the analyzer for detailed insights:

```bash
./analyze-test-results.sh
```

Output includes:

1. **Execution Summary** - Pass/fail counts and success rate
2. **Execution Times** - Slowest tests and total runtime
3. **Failure Analysis** - Detailed error information (if any)
4. **Common Error Patterns** - Known issues and their frequency
5. **Test Categories** - Coverage breakdown
6. **Upgrade Performance Metrics** - Operation durations
7. **Recommendations** - Actionable next steps

### Key Metrics to Watch

✅ **Success Rate**: Should be 100% for production readiness

⚠️ **Data Integrity Verifications**: All should pass

⚠️ **Upgrade Duration**: Typical times:
- Single DataNode upgrade: 5-15 seconds
- Full 3-node rolling upgrade: 30-60 seconds
- Large dataset (500MB): 2-5 minutes

⚠️ **Error Patterns**:
- Version incompatibility: Should be 0
- Data corruption: Should be 0 (critical)
- Connection errors: Should be minimal
- Timeouts: Occasional timeouts acceptable during node transitions

## Troubleshooting

### Common Issues

#### 1. Tests are Skipped

**Symptom:**
```
Tests run: 0, Failures: 0, Errors: 0, Skipped: 24
```

**Cause:** System properties not passed or Hadoop distributions not found

**Solution:**
```bash
# Verify system properties are being passed
mvn test -Dtest=TestHadoop335To336Upgrade \
  -Dhadoop.start.home=/tmp/hadoop-test-distributions/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/tmp/hadoop-test-distributions/hadoop-3.3.6

# Or run with automatic download
./run-upgrade-test.sh
```

**Note:** Tests now use system properties (`-Dhadoop.start.home` and `-Dhadoop.upgrade.home`) instead of environment variables.

#### 2. Download Failures

**Symptom:**
```
Failed to download Hadoop 3.3.5
```

**Cause:** Network issues or Apache mirror problems

**Solution:**
```bash
# Download manually
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.5/hadoop-3.3.5.tar.gz

# Or try different mirror
wget https://dlcdn.apache.org/hadoop/common/hadoop-3.3.5/hadoop-3.3.5.tar.gz
```

#### 3. Build Failures

**Symptom:**
```
BUILD FAILURE
```

**Cause:** Missing dependencies or Java version mismatch

**Solution:**
```bash
# Verify Java version
java -version  # Must be 1.8.x

# Clean and rebuild
mvn clean install -pl hadoop-hdfs-project/hadoop-hdfs -am -DskipTests
```

#### 4. Test Timeouts

**Symptom:**
```
Test timeout after 120000ms
```

**Cause:** System overload or slow I/O

**Solution:**
```bash
# Increase Maven timeout
export MAVEN_OPTS="-Xmx2048m"

# Or edit test to increase timeout in pom.xml
```

#### 5. Port Conflicts

**Symptom:**
```
java.net.BindException: Address already in use
```

**Cause:** Previous test cluster still running

**Solution:**
```bash
# Kill orphaned processes
pkill -f "ProcessLauncher"
pkill -f "NameNode"
pkill -f "DataNode"

# Or reboot system
```

#### 6. Disk Space Issues

**Symptom:**
```
No space left on device
```

**Cause:** Insufficient disk space for test data

**Solution:**
```bash
# Check disk space
df -h

# Clean up old test results
./run-upgrade-test.sh --clean

# Clean Maven cache
rm -rf ~/.m2/repository/org/apache/hadoop
```

### Debug Mode

For detailed debugging, run Maven with debug output:

```bash
mvn test \
  -pl hadoop-hdfs-project/hadoop-hdfs \
  -Dtest=TestHadoop335To336Upgrade \
  -X  # Debug mode
```

### Viewing Process Logs

ProcessBasedMiniDFSCluster creates process logs in `/tmp/process-minicluster-*`:

```bash
# List cluster instances
ls -la /tmp/process-minicluster-*

# View NameNode logs
tail -f /tmp/process-minicluster-*/nn0/logs/*.log

# View DataNode logs
tail -f /tmp/process-minicluster-*/dn0/logs/*.log
```

### Preserving Cluster Data for Debugging

By default, the cluster directory is cleaned up after tests complete. To preserve the cluster directory for debugging:

```bash
# Run tests with no cleanup
mvn test \
  -pl hadoop-hdfs-project/hadoop-hdfs \
  -Dtest=TestHadoop335To336Upgrade \
  -Dhadoop.start.home=/tmp/hadoop-test-distributions/hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/tmp/hadoop-test-distributions/hadoop-3.3.6 \
  -Dmini.dfs.no.cleanup=true
```

With `-Dmini.dfs.no.cleanup=true`, the cluster directory in `/tmp/process-minicluster-*` will be preserved after the test completes, allowing you to:

- Inspect HDFS data directories
- Review complete log files
- Examine configuration files used during the test
- Debug process startup issues

**Important:** Remember to manually clean up preserved directories when done:

```bash
# Clean up all preserved cluster directories
rm -rf /tmp/process-minicluster-*
```

## Advanced Usage

### Custom Test Data Sizes

Modify test data sizes by editing test code:

```java
// Small dataset
List<Path> files = UpgradeTestHelper.writeTestData(fs, 10, 1024 * 1024);  // 10 files, 1MB each

// Large dataset
List<Path> files = UpgradeTestHelper.writeTestData(fs, 1000, 10 * 1024 * 1024);  // 1000 files, 10MB each
```

### Custom Hadoop Distributions

Test with different Hadoop versions using system properties:

```bash
# Run tests with custom distribution paths
mvn test -pl hadoop-hdfs-project/hadoop-hdfs \
  -Dtest=TestHadoop335To336Upgrade \
  -Dhadoop.start.home=/opt/custom-hadoop-3.3.5 \
  -Dhadoop.upgrade.home=/opt/custom-hadoop-3.3.6

# Or with environment variables (fallback, but system properties preferred)
export HADOOP_HOME=/opt/custom-hadoop-3.3.5
export HADOOP_UPGRADE_HOME=/opt/custom-hadoop-3.3.6
./run-upgrade-test.sh --skip-download
```

### Parallel Test Execution

Run tests in parallel for faster execution:

```bash
mvn test \
  -pl hadoop-hdfs-project/hadoop-hdfs \
  -Dtest=org.apache.hadoop.hdfs.server.process.upgrade.* \
  -DforkCount=2
```

### CI/CD Integration

Example Jenkins/GitHub Actions configuration:

```yaml
# .github/workflows/upgrade-test.yml
name: Hadoop Upgrade Tests
on: [push, pull_request]

jobs:
  upgrade-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '8'
      - name: Run upgrade tests
        run: |
          ./run-upgrade-test.sh
      - name: Analyze results
        if: always()
        run: |
          ./analyze-test-results.sh
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: test-results-*
```

### Custom Test Configurations

Create custom HDFS configurations:

```java
Configuration conf = new HdfsConfiguration();
conf.set("dfs.replication", "2");
conf.set("dfs.block.size", "1048576");  // 1MB blocks
conf.set("dfs.namenode.heartbeat.recheck-interval", "5000");
```

## Best Practices

1. **Always run the full test suite** before production upgrades
2. **Test with production-like data volumes** (customize dataset sizes)
3. **Monitor system resources** during tests (CPU, memory, disk I/O)
4. **Save test results** for comparison across different versions
5. **Test both upgrade and downgrade** scenarios
6. **Verify mixed-version operation** if you plan phased rollouts
7. **Review all warnings** in test output, not just failures

## Resources

- [ProcessBasedMiniDFSCluster User Guide](hadoop-hdfs-project/hadoop-hdfs/docs/ProcessBasedMiniDFSCluster-UserGuide.md)
- [Version Upgrade Testing Guide](hadoop-hdfs-project/hadoop-hdfs/docs/VersionUpgradeTestingGuide.md)
- [ProcessBasedMiniDFSCluster Developer Guide](hadoop-hdfs-project/hadoop-hdfs/docs/ProcessBasedMiniDFSCluster-DeveloperGuide.md)
- [Apache Hadoop Rolling Upgrade Documentation](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/HdfsRollingUpgrade.html)

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review test logs in `test-results-*/test-execution.log`
3. Check process logs in `/tmp/process-minicluster-*/`
4. Consult the ProcessBasedMiniDFSCluster documentation

## License

Apache License 2.0 - See LICENSE file for details
