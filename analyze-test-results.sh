#!/bin/bash

################################################################################
# Hadoop Upgrade Test Results Analyzer
#
# This script analyzes test results from ProcessBasedMiniDFSCluster upgrade tests
# and generates detailed reports including failure analysis, performance metrics,
# and recommendations.
#
# Usage:
#   ./analyze-test-results.sh [TEST_RESULTS_DIR]
#
# If no directory is specified, it will find the most recent test-results-* directory.
#
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Determine test results directory
if [ -n "$1" ]; then
  TEST_RESULTS_DIR="$1"
else
  # Find most recent test-results directory
  TEST_RESULTS_DIR=$(find "$SCRIPT_DIR" -maxdepth 1 -type d -name "test-results-*" | sort -r | head -1)
fi

if [ -z "$TEST_RESULTS_DIR" ] || [ ! -d "$TEST_RESULTS_DIR" ]; then
  echo -e "${RED}Error: Test results directory not found${NC}"
  echo "Usage: $0 [TEST_RESULTS_DIR]"
  exit 1
fi

echo "======================================================================="
echo "  Hadoop Upgrade Test Results Analyzer"
echo "======================================================================="
echo ""
echo "Analyzing: $TEST_RESULTS_DIR"
echo ""

# Function to print section header
print_section() {
  echo ""
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${CYAN}  $1${NC}"
  echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Check if required files exist
EXECUTION_LOG="$TEST_RESULTS_DIR/test-execution.log"
SUREFIRE_DIR="$TEST_RESULTS_DIR/surefire-reports"

if [ ! -f "$EXECUTION_LOG" ]; then
  echo -e "${RED}Error: test-execution.log not found${NC}"
  exit 1
fi

# Parse basic test statistics
print_section "Test Execution Summary"

TESTS_RUN=$(grep -oP "Tests run: \K\d+" "$EXECUTION_LOG" | tail -1 || echo "0")
TESTS_FAILED=$(grep -oP "Failures: \K\d+" "$EXECUTION_LOG" | tail -1 || echo "0")
TESTS_ERROR=$(grep -oP "Errors: \K\d+" "$EXECUTION_LOG" | tail -1 || echo "0")
TESTS_SKIPPED=$(grep -oP "Skipped: \K\d+" "$EXECUTION_LOG" | tail -1 || echo "0")
TESTS_PASSED=$((TESTS_RUN - TESTS_FAILED - TESTS_ERROR - TESTS_SKIPPED))

echo "Tests run:     $TESTS_RUN"
echo "Tests passed:  $TESTS_PASSED"
echo "Tests failed:  $TESTS_FAILED"
echo "Tests error:   $TESTS_ERROR"
echo "Tests skipped: $TESTS_SKIPPED"

# Calculate success rate
if [ "$TESTS_RUN" -gt 0 ]; then
  SUCCESS_RATE=$(awk "BEGIN {printf \"%.2f\", ($TESTS_PASSED / $TESTS_RUN) * 100}")
  echo ""
  if (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${GREEN}Success Rate: ${SUCCESS_RATE}%${NC}"
  elif (( $(echo "$SUCCESS_RATE >= 70" | bc -l) )); then
    echo -e "${YELLOW}Success Rate: ${SUCCESS_RATE}%${NC}"
  else
    echo -e "${RED}Success Rate: ${SUCCESS_RATE}%${NC}"
  fi
fi

# Extract test execution times
print_section "Test Execution Times"

if [ -d "$SUREFIRE_DIR" ]; then
  echo "Analyzing test execution times from JUnit reports..."
  echo ""

  # Find all XML test reports
  TEST_TIMES=$(find "$SUREFIRE_DIR" -name "TEST-*.xml" -type f | while read xml_file; do
    test_name=$(basename "$xml_file" | sed 's/TEST-//' | sed 's/.xml//')
    test_time=$(grep -oP 'time="\K[0-9.]+' "$xml_file" | head -1 || echo "0")
    echo "$test_time $test_name"
  done | sort -rn)

  if [ -n "$TEST_TIMES" ]; then
    echo "Top 10 slowest tests:"
    echo "$TEST_TIMES" | head -10 | while read time name; do
      printf "  %8.3fs  %s\n" "$time" "$name"
    done
  fi

  # Calculate total test time
  TOTAL_TIME=$(echo "$TEST_TIMES" | awk '{sum += $1} END {print sum}')
  if [ -n "$TOTAL_TIME" ] && [ "$TOTAL_TIME" != "0" ]; then
    echo ""
    printf "Total test execution time: %.2fs (%.2f minutes)\n" "$TOTAL_TIME" "$(echo "$TOTAL_TIME / 60" | bc -l)"
  fi
else
  echo "Surefire reports not found, skipping execution time analysis"
fi

# Analyze failures
if [ "$TESTS_FAILED" -gt 0 ] || [ "$TESTS_ERROR" -gt 0 ]; then
  print_section "Failure Analysis"

  echo -e "${RED}Found $TESTS_FAILED failures and $TESTS_ERROR errors${NC}"
  echo ""

  # Extract failure details
  echo "Failed test cases:"
  echo ""

  grep -A 10 "<<< FAILURE!" "$EXECUTION_LOG" | while IFS= read -r line; do
    if [[ $line =~ "<<< FAILURE!" ]]; then
      echo -e "${RED}✗${NC} $line"
    elif [[ $line =~ "<<< ERROR!" ]]; then
      echo -e "${RED}✗${NC} $line"
    elif [[ ! -z "$line" ]]; then
      echo "  $line"
    fi
  done

  # Look for common error patterns
  print_section "Common Error Patterns"

  echo "Searching for common upgrade issues..."
  echo ""

  # Check for version incompatibility errors
  VERSION_ERRORS=$(grep -i "version.*incompatible\|protocol.*mismatch" "$EXECUTION_LOG" | wc -l)
  if [ "$VERSION_ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}⚠${NC}  Found $VERSION_ERRORS version incompatibility warnings"
  fi

  # Check for connection errors
  CONN_ERRORS=$(grep -i "connection.*refused\|failed to connect" "$EXECUTION_LOG" | wc -l)
  if [ "$CONN_ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}⚠${NC}  Found $CONN_ERRORS connection errors"
  fi

  # Check for timeout errors
  TIMEOUT_ERRORS=$(grep -i "timeout\|timed out" "$EXECUTION_LOG" | wc -l)
  if [ "$TIMEOUT_ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}⚠${NC}  Found $TIMEOUT_ERRORS timeout errors"
  fi

  # Check for data corruption
  DATA_ERRORS=$(grep -i "corrupt\|checksum.*failed\|data.*mismatch" "$EXECUTION_LOG" | wc -l)
  if [ "$DATA_ERRORS" -gt 0 ]; then
    echo -e "${RED}⚠${NC}  Found $DATA_ERRORS data corruption errors (CRITICAL)"
  fi

  # Check for process crashes
  CRASH_ERRORS=$(grep -i "process.*died\|process.*crashed\|exit code" "$EXECUTION_LOG" | wc -l)
  if [ "$CRASH_ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}⚠${NC}  Found $CRASH_ERRORS process crash errors"
  fi

  if [ "$VERSION_ERRORS" -eq 0 ] && [ "$CONN_ERRORS" -eq 0 ] && [ "$TIMEOUT_ERRORS" -eq 0 ] && [ "$DATA_ERRORS" -eq 0 ] && [ "$CRASH_ERRORS" -eq 0 ]; then
    echo "No common error patterns detected"
  fi
fi

# Analyze test categories
print_section "Test Categories"

if [ -d "$SUREFIRE_DIR" ]; then
  echo "Test coverage by category:"
  echo ""

  # Count tests by class
  ROLLING_UPGRADE=$(find "$SUREFIRE_DIR" -name "*TestRollingUpgrade*.xml" -o -name "*TestHadoop335To336Upgrade*.xml" | wc -l)
  MIXED_VERSION=$(find "$SUREFIRE_DIR" -name "*TestMixedVersion*.xml" | wc -l)
  UNIT_TESTS=$(find "$SUREFIRE_DIR" -name "*Test*.xml" | wc -l)

  echo "  Rolling upgrade tests:    $ROLLING_UPGRADE"
  echo "  Mixed version tests:      $MIXED_VERSION"
  echo "  Total test classes:       $UNIT_TESTS"
fi

# Extract upgrade-specific metrics
print_section "Upgrade Performance Metrics"

echo "Analyzing upgrade-specific operations..."
echo ""

# Look for upgrade duration logs
UPGRADE_DURATIONS=$(grep -oP "completed in \K\d+ms" "$EXECUTION_LOG" || true)
if [ -n "$UPGRADE_DURATIONS" ]; then
  echo "Upgrade operation durations:"
  echo "$UPGRADE_DURATIONS" | sort -n | while read duration; do
    duration_sec=$(echo "scale=2; $duration / 1000" | bc)
    echo "  ${duration_sec}s"
  done
else
  echo "No upgrade duration metrics found in logs"
fi

# Check for data verification
DATA_VERIFICATIONS=$(grep -c "Data verification.*PASSED\|verified.*files" "$EXECUTION_LOG" || echo "0")
if [ "$DATA_VERIFICATIONS" -gt 0 ]; then
  echo ""
  echo "Data integrity verifications: $DATA_VERIFICATIONS"
fi

# Generate recommendations
print_section "Recommendations"

if [ "$TESTS_FAILED" -eq 0 ] && [ "$TESTS_ERROR" -eq 0 ]; then
  echo -e "${GREEN}✓ All tests passed successfully!${NC}"
  echo ""
  echo "The upgrade from Hadoop 3.3.5 to 3.3.6 appears to be working correctly."
  echo ""
  echo "Next steps:"
  echo "  1. Review performance metrics above"
  echo "  2. Consider running additional stress tests"
  echo "  3. Test with production-like workloads"
  echo "  4. Plan rolling upgrade for production cluster"
else
  echo -e "${YELLOW}⚠ Some tests failed. Review required.${NC}"
  echo ""
  echo "Recommended actions:"

  if [ "$DATA_ERRORS" -gt 0 ]; then
    echo -e "  ${RED}1. CRITICAL: Investigate data corruption errors immediately${NC}"
    echo "     - Check HDFS logs for disk errors"
    echo "     - Verify checksums are enabled"
    echo "     - Review replication settings"
  fi

  if [ "$VERSION_ERRORS" -gt 0 ]; then
    echo "  2. Review version compatibility issues"
    echo "     - Verify both distributions are properly installed"
    echo "     - Check HADOOP_3_3_5_HOME and HADOOP_3_3_6_HOME environment variables"
  fi

  if [ "$CONN_ERRORS" -gt 0 ] || [ "$CRASH_ERRORS" -gt 0 ]; then
    echo "  3. Investigate process stability issues"
    echo "     - Check system resources (memory, disk space)"
    echo "     - Review process logs in test output directory"
    echo "     - Consider increasing timeouts"
  fi

  if [ "$TIMEOUT_ERRORS" -gt 0 ]; then
    echo "  4. Address timeout issues"
    echo "     - Increase test timeouts in configuration"
    echo "     - Check system load and performance"
    echo "     - Review cluster startup times"
  fi

  echo ""
  echo "Detailed logs available at:"
  echo "  $TEST_RESULTS_DIR/test-execution.log"
  if [ -d "$SUREFIRE_DIR" ]; then
    echo "  $SUREFIRE_DIR/"
  fi
fi

# Summary
print_section "Analysis Complete"

echo "Test results directory: $TEST_RESULTS_DIR"
echo ""

if [ "$TESTS_FAILED" -eq 0 ] && [ "$TESTS_ERROR" -eq 0 ]; then
  echo -e "${GREEN}Overall Status: SUCCESS ✓${NC}"
  exit 0
else
  echo -e "${RED}Overall Status: FAILURES DETECTED ✗${NC}"
  exit 1
fi
