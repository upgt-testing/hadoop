#!/bin/bash

# Script to run all YARN restart tests using the Restart Testing Framework
# This script will:
# 1. Find all modules with restart tests
# 2. Run restart-test:run for each module
# 3. Collect all outputs to a timestamped directory

set -e  # Exit on error

# Generate timestamp for output directory
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_DIR="restart-test-output-${TIMESTAMP}"

echo "=========================================="
echo "YARN Restart Testing Framework"
echo "=========================================="
echo "Output directory: ${OUTPUT_DIR}"
echo ""

# Create output directory
mkdir -p "${OUTPUT_DIR}"

# Define all modules with restart tests
MODULES=(
    "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-client"
    "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell"
    "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-unmanaged-am-launcher"
    "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-services/hadoop-yarn-services-core"
    "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-services/hadoop-yarn-services-api"
    "hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-tests"
)

# Track overall results
TOTAL_MODULES=${#MODULES[@]}
PASSED_MODULES=0
FAILED_MODULES=0
CURRENT_MODULE=0

echo "Found ${TOTAL_MODULES} modules with restart tests"
echo ""

# Run restart tests for each module
for MODULE in "${MODULES[@]}"; do
    CURRENT_MODULE=$((CURRENT_MODULE + 1))
    MODULE_NAME=$(basename "${MODULE}")
    echo "=========================================="
    echo "Module ${CURRENT_MODULE}/${TOTAL_MODULES}: ${MODULE_NAME}"
    echo "Path: ${MODULE}"
    echo "=========================================="

    # Create module-specific output directory
    MODULE_OUTPUT="${OUTPUT_DIR}/${MODULE_NAME}"
    mkdir -p "${MODULE_OUTPUT}"

    # Log file for this module
    LOG_FILE="${MODULE_OUTPUT}/test-execution.log"

    echo "Running restart tests..."
    echo "Log file: ${LOG_FILE}"
    echo "Start time: $(date '+%H:%M:%S')"
    echo ""
    echo "Maven output:"
    echo "------------------------------------------"

    # Record start time
    MODULE_START=$(date +%s)

    # Run the restart tests and capture full Maven output
    # Use tee to both display and log all output
    if (cd "${MODULE}" && mvn restart-test:run -Drestart.failOnError=false 2>&1) | tee "${LOG_FILE}"; then
        MODULE_END=$(date +%s)
        MODULE_DURATION=$((MODULE_END - MODULE_START))
        echo ""
        echo "✓ Module tests completed successfully (took ${MODULE_DURATION}s)"
        PASSED_MODULES=$((PASSED_MODULES + 1))

        # Copy report files if they exist
        if [ -d "${MODULE}/target/restart-reports" ]; then
            cp -r "${MODULE}/target/restart-reports"/* "${MODULE_OUTPUT}/" 2>/dev/null || true
            echo "  Reports copied to ${MODULE_OUTPUT}/"
        fi
    else
        MODULE_END=$(date +%s)
        MODULE_DURATION=$((MODULE_END - MODULE_START))
        echo ""
        echo "✗ Module tests failed or had errors (took ${MODULE_DURATION}s)"
        FAILED_MODULES=$((FAILED_MODULES + 1))

        # Still copy report files even if tests failed
        if [ -d "${MODULE}/target/restart-reports" ]; then
            cp -r "${MODULE}/target/restart-reports"/* "${MODULE_OUTPUT}/" 2>/dev/null || true
            echo "  Reports copied to ${MODULE_OUTPUT}/"
        fi
    fi

    echo ""
done

# Generate summary report
SUMMARY_FILE="${OUTPUT_DIR}/SUMMARY.txt"
cat > "${SUMMARY_FILE}" << EOF
========================================
YARN Restart Test Summary
========================================
Timestamp: ${TIMESTAMP}
Date: $(date)

Modules Tested: ${TOTAL_MODULES}
Passed: ${PASSED_MODULES}
Failed: ${FAILED_MODULES}

Module Results:
EOF

for MODULE in "${MODULES[@]}"; do
    MODULE_NAME=$(basename "${MODULE}")
    echo "  - ${MODULE_NAME}" >> "${SUMMARY_FILE}"
done

echo ""
cat "${SUMMARY_FILE}"

echo ""
echo "=========================================="
echo "Test Execution Complete"
echo "=========================================="
echo "All outputs saved to: ${OUTPUT_DIR}/"
echo "Summary: ${SUMMARY_FILE}"
echo ""

# List all HTML reports
echo "HTML Reports:"
find "${OUTPUT_DIR}" -name "*.html" -type f | while read -r report; do
    echo "  - ${report}"
done

echo ""

# Exit with error if any module failed
if [ ${FAILED_MODULES} -gt 0 ]; then
    echo "⚠ Warning: ${FAILED_MODULES} module(s) had test failures"
    echo "Check individual module logs and reports for details"
    exit 1
else
    echo "✓ All modules completed successfully"
    exit 0
fi
