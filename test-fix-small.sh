#!/bin/bash

# Small test script to verify the restart-maven-plugin fix
# This runs a limited number of tests to check if failures are properly detected

set -e

TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
OUTPUT_DIR="restart-test-output-fix-test-${TIMESTAMP}"

echo "=========================================="
echo "Testing Restart Maven Plugin Fix"
echo "=========================================="
echo "Output directory: ${OUTPUT_DIR}"
echo ""

mkdir -p "${OUTPUT_DIR}"

# Test only hadoop-yarn-client (has known failures)
MODULE="hadoop-yarn-project/hadoop-yarn/hadoop-yarn-client"
MODULE_NAME=$(basename "${MODULE}")

echo "=========================================="
echo "Module: ${MODULE_NAME}"
echo "=========================================="

MODULE_OUTPUT="${OUTPUT_DIR}/${MODULE_NAME}"
mkdir -p "${MODULE_OUTPUT}"

LOG_FILE="${MODULE_OUTPUT}/test-execution.log"

echo "Running restart tests..."
echo "Log file: ${LOG_FILE}"
echo ""

# Run the restart tests
if (cd "${MODULE}" && mvn restart-test:run -Drestart.failOnError=false) > "${LOG_FILE}" 2>&1; then
    echo "✓ Plugin execution completed"
else
    echo "✗ Plugin execution had errors (expected if tests fail)"
fi

# Copy report files
if [ -d "${MODULE}/target/restart-reports" ]; then
    cp -r "${MODULE}/target/restart-reports"/* "${MODULE_OUTPUT}/" 2>/dev/null || true
    echo "Reports copied to ${MODULE_OUTPUT}/"
fi

echo ""
echo "=========================================="
echo "Verification"
echo "=========================================="

# Check if report exists
if [ -f "${MODULE_OUTPUT}/restart-test-report.json" ]; then
    echo "✓ JSON report generated"

    # Extract key metrics from JSON
    TOTAL=$(grep -o '"totalTests":[0-9]*' "${MODULE_OUTPUT}/restart-test-report.json" | head -1 | cut -d: -f2)
    PASSED=$(grep -o '"passed":[0-9]*' "${MODULE_OUTPUT}/restart-test-report.json" | head -1 | cut -d: -f2)
    FAILED=$(grep -o '"failed":[0-9]*' "${MODULE_OUTPUT}/restart-test-report.json" | head -1 | cut -d: -f2)

    echo "  Total tests: ${TOTAL}"
    echo "  Passed: ${PASSED}"
    echo "  Failed: ${FAILED}"

    if [ "${FAILED}" -gt 0 ]; then
        echo ""
        echo "✓ SUCCESS: Plugin correctly detected ${FAILED} failures!"
        echo "  (Previously all tests were incorrectly reported as passed)"
    else
        echo ""
        echo "⚠ WARNING: No failures detected in report"
        echo "  This might indicate the fix didn't work, or all tests actually passed"
    fi
else
    echo "✗ JSON report not found"
fi

echo ""
echo "Test output saved to: ${OUTPUT_DIR}/"
echo "View the report at: ${MODULE_OUTPUT}/restart-test-report.html"
echo ""
