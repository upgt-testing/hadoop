#!/bin/bash
# Script to collect restart test results

echo "=== Collecting Restart Test Results ==="
echo "Timestamp: $(date)"

# Create results directory if it doesn't exist
mkdir -p restart-test-results

# Copy surefire reports if they exist
if [ -d "target/surefire-reports" ]; then
    echo "Copying surefire reports..."
    cp -r target/surefire-reports restart-test-results/
    echo "  ✓ Surefire reports copied"
else
    echo "  ⚠ No surefire reports found"
fi

# Create summary of test results
echo "Creating test summary..."
cat > restart-test-results/TEST-SUMMARY.md << 'EOF'
# Restart Testing - Test Execution Summary

**Execution Date:** $(date)
**Hadoop Version:** 3.3.5

## Test Suites Executed

### 1. TestFileCreationWithRestart (5 tests)
- testSimpleFileWriteWithNameNodeRestart
- testFileWriteWithNameNodeCrash
- testMultiBlockWriteWithRestart
- testMinimalWriteRestart
- testLeaseRecoveryAfterRestart

### 2. TestAppendWithRestart (6 tests)
- testSimpleAppendWithDataNodeRestart
- testAppendWithDataNodeCrash
- testAppendAtBlockBoundaryWithRestart
- testMultipleAppendsWithRestarts
- testAppendWithAllDataNodesRestart

### 3. TestReplicationWithRestart (6 tests)
- testIncreaseReplicationWithDataNodeRestart
- testDecreaseReplicationWithDataNodeRestart
- testReplicationWithRollingDataNodeRestarts
- testReplicationWithNameNodeRestart
- testUnderReplicationRecoveryAfterCrash
- testReplicationWithBothNNAndDNRestart

**Total Tests:** 17

## Test Results

EOF

# Extract test results from surefire reports if available
if [ -d "restart-test-results/surefire-reports" ]; then
    echo "Analyzing surefire reports..."

    # Count tests
    TOTAL=$(find restart-test-results/surefire-reports -name "TEST-*.xml" -exec grep -h "tests=" {} \; | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
    FAILURES=$(find restart-test-results/surefire-reports -name "TEST-*.xml" -exec grep -h "failures=" {} \; | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
    ERRORS=$(find restart-test-results/surefire-reports -name "TEST-*.xml" -exec grep -h "errors=" {} \; | sed 's/.*errors="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
    SKIPPED=$(find restart-test-results/surefire-reports -name "TEST-*.xml" -exec grep -h "skipped=" {} \; | sed 's/.*skipped="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')

    cat >> restart-test-results/TEST-SUMMARY.md << EOF

- **Total Tests Run:** ${TOTAL:-0}
- **Failures:** ${FAILURES:-0}
- **Errors:** ${ERRORS:-0}
- **Skipped:** ${SKIPPED:-0}
- **Passed:** $((${TOTAL:-0} - ${FAILURES:-0} - ${ERRORS:-0} - ${SKIPPED:-0}))

## Detailed Results

See \`surefire-reports/\` directory for detailed XML and TXT reports.

## Log Files

- \`full-test-run.log\` - Complete Maven test execution output
- \`test-execution-log.txt\` - Execution timestamp

EOF

    echo "  ✓ Test summary created"
fi

# List all collected files
echo ""
echo "=== Files Collected ==="
ls -lh restart-test-results/

echo ""
echo "✅ Test result collection complete!"
echo "Results are in: restart-test-results/"
