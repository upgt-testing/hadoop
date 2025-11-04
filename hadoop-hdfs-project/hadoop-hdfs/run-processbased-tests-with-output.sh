#!/bin/bash

# Script to find, cache, and run all ProcessBased tests
# Usage: ./run-processbased-tests-with-output.sh [start_home] [upgrade_home]

set -e

# Default Hadoop distribution paths
DEFAULT_START_HOME="/Users/allenwang/xlab/hadoop-test-distributions/hadoop-3.3.5"
DEFAULT_UPGRADE_HOME="/Users/allenwang/xlab/hadoop-test-distributions/hadoop-3.4.0"

# Use provided arguments or defaults
HADOOP_START_HOME="${1:-$DEFAULT_START_HOME}"
HADOOP_UPGRADE_HOME="${2:-$DEFAULT_UPGRADE_HOME}"

# File to cache test class names
TEST_CACHE_FILE="processbased_tests_cache.txt"

# Output directory for test logs
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="test-output-${TIMESTAMP}"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ProcessBased Test Runner${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo -e "  Start Home:   ${HADOOP_START_HOME}"
echo -e "  Upgrade Home: ${HADOOP_UPGRADE_HOME}"
echo -e "  Output Dir:   ${OUTPUT_DIR}"
echo ""

# Create output directory
mkdir -p "${OUTPUT_DIR}"
echo -e "${GREEN}✓ Created output directory: ${OUTPUT_DIR}${NC}"
echo ""

# Find or load cached test classes
if [ ! -f "${TEST_CACHE_FILE}" ] || [ "$FORCE_REFRESH" = "1" ]; then
    echo -e "${BLUE}Finding all *_ProcessBased test classes...${NC}"

    # Find all test classes ending with _ProcessBased.java
    find src/test/java -type f -name "*_ProcessBased.java" | while read -r filepath; do
        # Extract the fully qualified class name
        # Remove src/test/java/ prefix and .java suffix, then replace / with .
        classname=$(echo "$filepath" | sed 's|src/test/java/||' | sed 's|\.java$||' | tr '/' '.')
        echo "$classname"
    done | sort > "${TEST_CACHE_FILE}"

    TEST_COUNT=$(wc -l < "${TEST_CACHE_FILE}" | tr -d ' ')
    echo -e "${GREEN}✓ Found ${TEST_COUNT} test classes and saved to ${TEST_CACHE_FILE}${NC}"
else
    TEST_COUNT=$(wc -l < "${TEST_CACHE_FILE}" | tr -d ' ')
    echo -e "${GREEN}✓ Loaded ${TEST_COUNT} test classes from cache: ${TEST_CACHE_FILE}${NC}"
    echo -e "  ${YELLOW}(To refresh cache, run: FORCE_REFRESH=1 $0)${NC}"
fi

echo ""

# Read test classes and run them
test_num=0
passed=0
failed=0
failed_tests=""

while IFS= read -r test_class; do
    test_num=$((test_num + 1))

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}[${test_num}/${TEST_COUNT}] Running: ${test_class}${NC}"
    echo -e "${BLUE}========================================${NC}"

    # Create a safe filename from the test class name
    safe_filename=$(echo "${test_class}" | tr '.' '_')
    log_file="${OUTPUT_DIR}/${safe_filename}.log"

    # Run the test and tee output to both console and file
    if mvn surefire:test \
        -Dtest="${test_class}" \
        -Dhadoop.start.home="${HADOOP_START_HOME}" \
        -Dhadoop.upgrade.home="${HADOOP_UPGRADE_HOME}" \
        2>&1 | tee "${log_file}"; then

        echo -e "${GREEN}✓ PASSED: ${test_class}${NC}"
        passed=$((passed + 1))
    else
        echo -e "${RED}✗ FAILED: ${test_class}${NC}"
        failed=$((failed + 1))
        failed_tests="${failed_tests}\n  - ${test_class}"
    fi

    echo ""
done < "${TEST_CACHE_FILE}"

# Print summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Execution Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Total Tests:  ${TEST_COUNT}"
echo -e "${GREEN}Passed:       ${passed}${NC}"
if [ ${failed} -gt 0 ]; then
    echo -e "${RED}Failed:       ${failed}${NC}"
    echo -e "${RED}Failed Tests:${failed_tests}${NC}"
else
    echo -e "Failed:       ${failed}"
fi
echo ""
echo -e "${BLUE}Output Directory: ${OUTPUT_DIR}${NC}"
echo -e "  - Individual test logs saved"
echo ""

# Create a summary file
summary_file="${OUTPUT_DIR}/SUMMARY.txt"
{
    echo "ProcessBased Test Execution Summary"
    echo "===================================="
    echo ""
    echo "Timestamp: ${TIMESTAMP}"
    echo "Start Home: ${HADOOP_START_HOME}"
    echo "Upgrade Home: ${HADOOP_UPGRADE_HOME}"
    echo ""
    echo "Results:"
    echo "  Total:  ${TEST_COUNT}"
    echo "  Passed: ${passed}"
    echo "  Failed: ${failed}"
    if [ ${failed} -gt 0 ]; then
        echo ""
        echo "Failed Tests:"
        echo -e "${failed_tests}"
    fi
} > "${summary_file}"

echo -e "${GREEN}✓ Summary saved to: ${summary_file}${NC}"

# Exit with appropriate code
if [ ${failed} -gt 0 ]; then
    exit 1
else
    exit 0
fi
