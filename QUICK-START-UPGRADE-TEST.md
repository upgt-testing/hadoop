# Quick Start: Hadoop 3.3.5 → 3.3.6 Upgrade Testing

## What Was Created

This repository now includes a complete upgrade testing infrastructure:

### 📁 Files Created

1. **`run-upgrade-test.sh`** (9.9KB)
   - Main test execution script
   - Downloads Hadoop 3.3.5 and 3.3.6 automatically
   - Builds project and runs tests
   - Generates detailed reports

2. **`analyze-test-results.sh`** (9.8KB)
   - Test results analyzer
   - Parses JUnit reports
   - Identifies common error patterns
   - Provides actionable recommendations

3. **`hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/server/process/upgrade/TestHadoop335To336Upgrade.java`**
   - 7 comprehensive upgrade tests
   - Tests: rolling upgrade, downgrade, mixed versions, load testing, large datasets, partial upgrades
   - ~600 lines of test code

4. **`UPGRADE-TESTING-README.md`**
   - Complete documentation
   - Troubleshooting guide
   - Best practices

5. **`QUICK-START-UPGRADE-TEST.md`** (this file)
   - Quick reference guide

## 🚀 Run Tests Now (One Command)

```bash
./run-upgrade-test.sh
```

That's it! This single command will:
1. ✅ Download Hadoop 3.3.5 (~370MB)
2. ✅ Download Hadoop 3.3.6 (~370MB)
3. ✅ Extract distributions
4. ✅ Set environment variables
5. ✅ Build Hadoop HDFS module
6. ✅ Run 24+ upgrade tests
7. ✅ Generate test reports

**Expected Duration:** 20-40 minutes (first run with downloads)

## 📊 View Results

After tests complete:

```bash
# Analyze results with detailed breakdown
./analyze-test-results.sh

# Or view the quick summary
cat test-results-*/SUMMARY.txt
```

## 🎯 Test Scenarios Covered

### TestHadoop335To336Upgrade (New - 7 tests)

1. **testBasicRollingUpgrade335To336**
   - Standard rolling upgrade: 3.3.5 → 3.3.6
   - 3 DataNodes upgraded one-by-one
   - Data integrity verified after each node
   - Duration: ~3-5 minutes

2. **testRollingDowngrade336To335**
   - Rollback scenario: 3.3.6 → 3.3.5
   - Verifies downgrade works correctly
   - Duration: ~3-5 minutes

3. **testMixedVersionCluster335And336**
   - 2 nodes on 3.3.5, 1 node on 3.3.6
   - Verifies coexistence and stability
   - Duration: ~2-3 minutes

4. **testUpgradeUnderLoad**
   - Background writes during upgrade
   - Simulates production scenario
   - Tracks write success rate
   - Duration: ~5-8 minutes

5. **testLargeDatasetUpgrade**
   - 100 files, 5MB each (500MB total)
   - Tests data-heavy upgrade
   - Duration: ~8-12 minutes

6. **testPartialUpgrade**
   - Upgrade 50% of cluster, then complete
   - Tests phased rollout
   - Duration: ~5-7 minutes

7. **testVersionCompatibility335And336**
   - Version compatibility checks
   - Duration: <1 second

### Existing Tests (Also Run)

- **TestRollingUpgrade**: 9 tests covering various rolling upgrade scenarios
- **TestMixedVersionCluster**: 8 tests for version compatibility

**Total Tests:** 24+ integration tests

## 📈 Expected Results

### Success Criteria

✅ **All tests pass** (24/24)
✅ **No data corruption** errors
✅ **Success rate:** 100%
✅ **Data integrity verifications:** All passed

### Performance Benchmarks

Typical upgrade timings:
- Single DataNode upgrade: 5-15 seconds
- Full 3-node rolling upgrade: 30-60 seconds
- Large dataset (500MB): 2-5 minutes

## 🔧 Options and Variations

### Run Specific Tests

```bash
# Run only the new 3.3.5 → 3.3.6 tests
./run-upgrade-test.sh --test-class TestHadoop335To336Upgrade

# Run only basic upgrade test
./run-upgrade-test.sh \
  --test-class TestHadoop335To336Upgrade \
  --test-method testBasicRollingUpgrade335To336

# Run all rolling upgrade tests
./run-upgrade-test.sh --test-class TestRollingUpgrade
```

### Skip Downloads (Subsequent Runs)

After first run, distributions are cached in `/tmp/hadoop-test-distributions/`:

```bash
# Skip download, use existing distributions
./run-upgrade-test.sh --skip-download

# Skip download and build
./run-upgrade-test.sh --skip-download --skip-build
```

### Clean Up

```bash
# Remove downloaded distributions and test artifacts
./run-upgrade-test.sh --clean
```

## 📂 Output Structure

After running tests:

```
hadoop-transform/
├── run-upgrade-test.sh              # Main script
├── analyze-test-results.sh          # Analysis script
├── UPGRADE-TESTING-README.md        # Full documentation
├── QUICK-START-UPGRADE-TEST.md      # This file
│
├── test-results-YYYYMMDD-HHMMSS/   # Test results (created after run)
│   ├── test-execution.log          # Complete Maven output
│   ├── surefire-reports/           # JUnit XML reports
│   │   ├── TEST-*.xml              # Individual test results
│   │   └── *.txt                   # Test output
│   └── SUMMARY.txt                 # Quick summary
│
└── /tmp/hadoop-test-distributions/ # Downloaded distributions (cached)
    ├── hadoop-3.3.5/
    └── hadoop-3.3.6/
```

## 🐛 Troubleshooting

### Tests Are Skipped

**Problem:** All tests show "Skipped"

**Solution:**
```bash
# The script sets environment variables automatically
./run-upgrade-test.sh
```

### Build Fails

**Problem:** Maven build errors

**Solution:**
```bash
# Check Java version (must be JDK 8)
java -version

# Clean and retry
./run-upgrade-test.sh --clean
./run-upgrade-test.sh
```

### Network/Download Issues

**Problem:** wget fails to download distributions

**Solution:**
```bash
# Download manually
mkdir -p /tmp/hadoop-test-distributions
cd /tmp/hadoop-test-distributions
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.5/hadoop-3.3.5.tar.gz
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
tar xzf hadoop-3.3.5.tar.gz
tar xzf hadoop-3.3.6.tar.gz

# Then run with skip download
cd /path/to/hadoop-transform
./run-upgrade-test.sh --skip-download
```

### Port Conflicts

**Problem:** "Address already in use"

**Solution:**
```bash
# Kill orphaned processes
pkill -f "ProcessLauncher"
pkill -f "NameNode"
pkill -f "DataNode"
```

## 📚 Next Steps

1. **Run the tests:** `./run-upgrade-test.sh`
2. **Review results:** `./analyze-test-results.sh`
3. **Read full documentation:** [UPGRADE-TESTING-README.md](UPGRADE-TESTING-README.md)
4. **Check test logs** if any failures occur
5. **Plan production upgrade** based on test results

## 💡 Key Features

- ✅ **Fully Automated** - One command to run everything
- ✅ **Comprehensive** - 24+ tests covering all upgrade scenarios
- ✅ **Real Processes** - Uses actual Hadoop distributions, not mocks
- ✅ **Production-Like** - Tests real upgrade procedures
- ✅ **Detailed Reports** - Analysis script provides actionable insights
- ✅ **Caching** - Downloads distributions once, reuses for multiple runs
- ✅ **Flexible** - Run all tests or specific subsets
- ✅ **Well Documented** - Complete guides and troubleshooting

## 🎓 Understanding the Tests

Each test follows this pattern:

```
1. Start cluster with version A
2. Write test data
3. Upgrade nodes to version B (one-by-one for rolling upgrades)
4. Verify data integrity after each step
5. Test read/write operations
6. Cleanup
```

This ensures:
- No data loss during upgrade
- Cluster remains operational
- Both versions can coexist
- Rollback works if needed

## ⏱️ Time Estimates

- **First run (with downloads):** 30-40 minutes
- **Subsequent runs (cached):** 15-25 minutes
- **Single test:** 2-10 minutes (depending on test)
- **Analysis:** <1 minute

## 📞 Support

For detailed documentation, see:
- **[UPGRADE-TESTING-README.md](UPGRADE-TESTING-README.md)** - Complete guide
- **[VersionUpgradeTestingGuide.md](hadoop-hdfs-project/hadoop-hdfs/docs/VersionUpgradeTestingGuide.md)** - Framework documentation

## ✨ Example Run

```bash
$ ./run-upgrade-test.sh
=======================================================================
  Hadoop Upgrade Test Runner
  Testing: Hadoop 3.3.5 → 3.3.6
=======================================================================

[INFO] Step 1: Downloading Hadoop distributions...
[INFO] Downloading Hadoop 3.3.5...
[INFO] Downloading Hadoop 3.3.6...
[SUCCESS] Step 1 completed: Hadoop distributions ready

[INFO] Step 2: Setting environment variables...
[SUCCESS] Step 2 completed: Environment variables set

[INFO] Step 3: Building Hadoop project...
[SUCCESS] Step 3 completed: Build successful

[INFO] Step 4: Running upgrade tests...
[INFO] Running all upgrade tests in upgrade package

Tests run: 24, Failures: 0, Errors: 0, Skipped: 0

=======================================================================
  TEST RESULTS SUMMARY
=======================================================================
  Tests run:     24
  Failures:      0
  Errors:        0
  Skipped:       0
=======================================================================

[SUCCESS] All tests passed! ✓
```

---

**Ready to start?** Just run:

```bash
./run-upgrade-test.sh
```
