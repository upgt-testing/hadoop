#!/bin/bash

################################################################################
# Hadoop Upgrade Test Runner
#
# This script automates the process of testing Hadoop version upgrades using
# ProcessBasedMiniDFSCluster framework. It downloads required Hadoop distributions,
# builds the test framework, and executes comprehensive upgrade tests.
#
# Usage:
#   ./run-upgrade-test.sh [OPTIONS]
#
# Options:
#   --skip-download    Skip downloading Hadoop distributions (use existing)
#   --skip-build       Skip building Hadoop project
#   --test-class NAME  Run specific test class (default: all upgrade tests)
#   --test-method NAME Run specific test method
#   --clean            Clean up test distributions and build artifacts
#   --help             Show this help message
#
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DOWNLOAD_DIR="${DOWNLOAD_DIR:-/tmp/hadoop-test-distributions}"
HADOOP_VERSION_335="3.3.5"
HADOOP_VERSION_336="3.3.6"
HADOOP_MIRROR="https://archive.apache.org/dist/hadoop/common"
TEST_MODULE="hadoop-hdfs-project/hadoop-hdfs"

# Parse command line arguments
SKIP_DOWNLOAD=false
SKIP_BUILD=false
TEST_CLASS=""
TEST_METHOD=""
CLEAN_MODE=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --skip-download)
      SKIP_DOWNLOAD=true
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --test-class)
      TEST_CLASS="$2"
      shift 2
      ;;
    --test-method)
      TEST_METHOD="$2"
      shift 2
      ;;
    --clean)
      CLEAN_MODE=true
      shift
      ;;
    --help)
      head -n 21 "$0" | tail -n 15
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      echo "Use --help for usage information"
      exit 1
      ;;
  esac
done

# Logging functions
log_info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
  echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# Clean mode
if [ "$CLEAN_MODE" = true ]; then
  log_info "Cleaning up test distributions and build artifacts..."

  if [ -d "$DOWNLOAD_DIR" ]; then
    log_info "Removing $DOWNLOAD_DIR"
    rm -rf "$DOWNLOAD_DIR"
  fi

  log_info "Cleaning Maven artifacts..."
  cd "$SCRIPT_DIR"
  mvn clean -pl "$TEST_MODULE" -q

  log_success "Cleanup completed"
  exit 0
fi

# Print banner
echo "======================================================================="
echo "  Hadoop Upgrade Test Runner"
echo "  Testing: Hadoop ${HADOOP_VERSION_335} → ${HADOOP_VERSION_336}"
echo "======================================================================="
echo ""

# Step 1: Download Hadoop distributions
if [ "$SKIP_DOWNLOAD" = false ]; then
  log_info "Step 1: Downloading Hadoop distributions..."

  mkdir -p "$DOWNLOAD_DIR"
  cd "$DOWNLOAD_DIR"

  # Download Hadoop 3.3.5
  HADOOP_335_DIR="$DOWNLOAD_DIR/hadoop-${HADOOP_VERSION_335}"
  if [ -d "$HADOOP_335_DIR" ]; then
    log_warn "Hadoop ${HADOOP_VERSION_335} already exists at $HADOOP_335_DIR, skipping download"
  else
    log_info "Downloading Hadoop ${HADOOP_VERSION_335}..."
    HADOOP_335_TAR="hadoop-${HADOOP_VERSION_335}.tar.gz"
    HADOOP_335_URL="${HADOOP_MIRROR}/hadoop-${HADOOP_VERSION_335}/${HADOOP_335_TAR}"

    if ! wget -q --show-progress "$HADOOP_335_URL"; then
      log_error "Failed to download Hadoop ${HADOOP_VERSION_335}"
      exit 1
    fi

    log_info "Extracting Hadoop ${HADOOP_VERSION_335}..."
    tar xzf "$HADOOP_335_TAR"
    rm "$HADOOP_335_TAR"
    log_success "Hadoop ${HADOOP_VERSION_335} ready at $HADOOP_335_DIR"
  fi

  # Download Hadoop 3.3.6
  HADOOP_336_DIR="$DOWNLOAD_DIR/hadoop-${HADOOP_VERSION_336}"
  if [ -d "$HADOOP_336_DIR" ]; then
    log_warn "Hadoop ${HADOOP_VERSION_336} already exists at $HADOOP_336_DIR, skipping download"
  else
    log_info "Downloading Hadoop ${HADOOP_VERSION_336}..."
    HADOOP_336_TAR="hadoop-${HADOOP_VERSION_336}.tar.gz"
    HADOOP_336_URL="${HADOOP_MIRROR}/hadoop-${HADOOP_VERSION_336}/${HADOOP_336_TAR}"

    if ! wget -q --show-progress "$HADOOP_336_URL"; then
      log_error "Failed to download Hadoop ${HADOOP_VERSION_336}"
      exit 1
    fi

    log_info "Extracting Hadoop ${HADOOP_VERSION_336}..."
    tar xzf "$HADOOP_336_TAR"
    rm "$HADOOP_336_TAR"
    log_success "Hadoop ${HADOOP_VERSION_336} ready at $HADOOP_336_DIR"
  fi

  log_success "Step 1 completed: Hadoop distributions ready"
else
  log_info "Step 1: Skipping download (--skip-download specified)"
  HADOOP_335_DIR="$DOWNLOAD_DIR/hadoop-${HADOOP_VERSION_335}"
  HADOOP_336_DIR="$DOWNLOAD_DIR/hadoop-${HADOOP_VERSION_336}"

  # Verify distributions exist
  if [ ! -d "$HADOOP_335_DIR" ]; then
    log_error "Hadoop ${HADOOP_VERSION_335} not found at $HADOOP_335_DIR"
    exit 1
  fi
  if [ ! -d "$HADOOP_336_DIR" ]; then
    log_error "Hadoop ${HADOOP_VERSION_336} not found at $HADOOP_336_DIR"
    exit 1
  fi
fi

echo ""

# Step 2: Set environment variables
log_info "Step 2: Setting environment variables..."
export HADOOP_3_3_5_HOME="$HADOOP_335_DIR"
export HADOOP_3_3_6_HOME="$HADOOP_336_DIR"
export HADOOP_HOME="$HADOOP_335_DIR"  # Default to 3.3.5

log_info "HADOOP_3_3_5_HOME=$HADOOP_3_3_5_HOME"
log_info "HADOOP_3_3_6_HOME=$HADOOP_3_3_6_HOME"
log_info "HADOOP_HOME=$HADOOP_HOME"
log_success "Step 2 completed: Environment variables set"

echo ""

# Step 3: Build Hadoop project
if [ "$SKIP_BUILD" = false ]; then
  log_info "Step 3: Building Hadoop project with ProcessBasedMiniDFSCluster..."
  cd "$SCRIPT_DIR"

  log_info "Building HDFS module (this may take several minutes)..."
  if ! mvn install -pl "$TEST_MODULE" -am -DskipTests -Dmaven.javadoc.skip=true -q; then
    log_error "Build failed"
    exit 1
  fi

  log_success "Step 3 completed: Build successful"
else
  log_info "Step 3: Skipping build (--skip-build specified)"
fi

echo ""

# Step 4: Run upgrade tests
log_info "Step 4: Running upgrade tests..."
cd "$SCRIPT_DIR"

# Determine which tests to run
TEST_PATTERN=""
if [ -n "$TEST_CLASS" ]; then
  if [ -n "$TEST_METHOD" ]; then
    TEST_PATTERN="$TEST_CLASS#$TEST_METHOD"
    log_info "Running specific test: $TEST_PATTERN"
  else
    TEST_PATTERN="$TEST_CLASS"
    log_info "Running test class: $TEST_PATTERN"
  fi
else
  # Run all upgrade tests (Maven Surefire requires explicit class names)
  TEST_PATTERN="TestRollingUpgrade,TestMixedVersionCluster,TestHadoop335To336Upgrade"
  log_info "Running all upgrade tests: TestRollingUpgrade, TestMixedVersionCluster, TestHadoop335To336Upgrade"
fi

# Create test output directory
TEST_OUTPUT_DIR="$SCRIPT_DIR/test-results-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$TEST_OUTPUT_DIR"

log_info "Test output will be saved to: $TEST_OUTPUT_DIR"
log_info "Starting test execution (this may take 10-30 minutes)..."
echo ""

# Run tests with environment variables
# Note: Maven Surefire runs in a separate JVM, so we pass env vars as system properties
# The test code checks both environment variables and system properties
set +e  # Don't exit on test failures
mvn test \
  -pl "$TEST_MODULE" \
  -Dtest="$TEST_PATTERN" \
  -Dmaven.javadoc.skip=true \
  -DHADOOP_HOME="$HADOOP_HOME" \
  -DHADOOP_3_3_5_HOME="$HADOOP_3_3_5_HOME" \
  -DHADOOP_3_3_6_HOME="$HADOOP_3_3_6_HOME" \
  2>&1 | tee "$TEST_OUTPUT_DIR/test-execution.log"

TEST_EXIT_CODE=${PIPESTATUS[0]}
set -e

echo ""

# Step 5: Analyze results
log_info "Step 5: Analyzing test results..."

# Copy test results
SUREFIRE_REPORTS="$SCRIPT_DIR/$TEST_MODULE/target/surefire-reports"
if [ -d "$SUREFIRE_REPORTS" ]; then
  cp -r "$SUREFIRE_REPORTS" "$TEST_OUTPUT_DIR/"
  log_info "Test reports copied to $TEST_OUTPUT_DIR/surefire-reports/"
fi

# Count test results
if [ -f "$TEST_OUTPUT_DIR/test-execution.log" ]; then
  TESTS_RUN=$(grep -E "Tests run: [0-9]+" "$TEST_OUTPUT_DIR/test-execution.log" | tail -1 | sed -E 's/.*Tests run: ([0-9]+).*/\1/' || echo "0")
  TESTS_FAILED=$(grep -E "Failures: [0-9]+" "$TEST_OUTPUT_DIR/test-execution.log" | tail -1 | sed -E 's/.*Failures: ([0-9]+).*/\1/' || echo "0")
  TESTS_ERROR=$(grep -E "Errors: [0-9]+" "$TEST_OUTPUT_DIR/test-execution.log" | tail -1 | sed -E 's/.*Errors: ([0-9]+).*/\1/' || echo "0")
  TESTS_SKIPPED=$(grep -E "Skipped: [0-9]+" "$TEST_OUTPUT_DIR/test-execution.log" | tail -1 | sed -E 's/.*Skipped: ([0-9]+).*/\1/' || echo "0")

  # Ensure all variables have valid numeric values
  TESTS_RUN=${TESTS_RUN:-0}
  TESTS_FAILED=${TESTS_FAILED:-0}
  TESTS_ERROR=${TESTS_ERROR:-0}
  TESTS_SKIPPED=${TESTS_SKIPPED:-0}

  echo ""
  echo "======================================================================="
  echo "  TEST RESULTS SUMMARY"
  echo "======================================================================="
  echo "  Tests run:     $TESTS_RUN"
  echo "  Failures:      $TESTS_FAILED"
  echo "  Errors:        $TESTS_ERROR"
  echo "  Skipped:       $TESTS_SKIPPED"
  echo "======================================================================="
  echo ""

  if [ "$TESTS_FAILED" -eq 0 ] && [ "$TESTS_ERROR" -eq 0 ]; then
    log_success "All tests passed! ✓"
  else
    log_error "Some tests failed. Check logs at: $TEST_OUTPUT_DIR"
  fi
fi

# Create summary report
SUMMARY_FILE="$TEST_OUTPUT_DIR/SUMMARY.txt"
cat > "$SUMMARY_FILE" <<EOF
=======================================================================
Hadoop Upgrade Test Summary
=======================================================================

Test Execution Date: $(date)
Source Version:      Hadoop ${HADOOP_VERSION_335}
Target Version:      Hadoop ${HADOOP_VERSION_336}

Environment:
  HADOOP_3_3_5_HOME: $HADOOP_3_3_5_HOME
  HADOOP_3_3_6_HOME: $HADOOP_3_3_6_HOME

Test Results:
  Tests run:     $TESTS_RUN
  Failures:      $TESTS_FAILED
  Errors:        $TESTS_ERROR
  Skipped:       $TESTS_SKIPPED

Test Pattern: $TEST_PATTERN

Output Directory: $TEST_OUTPUT_DIR

=======================================================================

EOF

if [ "$TESTS_FAILED" -eq 0 ] && [ "$TESTS_ERROR" -eq 0 ]; then
  echo "STATUS: SUCCESS ✓" >> "$SUMMARY_FILE"
else
  echo "STATUS: FAILED ✗" >> "$SUMMARY_FILE"
  echo "" >> "$SUMMARY_FILE"
  echo "Failed Tests:" >> "$SUMMARY_FILE"
  grep -A 2 "<<< FAILURE!" "$TEST_OUTPUT_DIR/test-execution.log" >> "$SUMMARY_FILE" 2>/dev/null || true
  grep -A 2 "<<< ERROR!" "$TEST_OUTPUT_DIR/test-execution.log" >> "$SUMMARY_FILE" 2>/dev/null || true
fi

cat "$SUMMARY_FILE"

log_info "Full test summary saved to: $SUMMARY_FILE"
log_info "Full test logs saved to: $TEST_OUTPUT_DIR/test-execution.log"

if [ -d "$SUREFIRE_REPORTS" ]; then
  log_info "JUnit XML reports available at: $TEST_OUTPUT_DIR/surefire-reports/"
fi

echo ""
log_success "Test execution completed!"

# Exit with test exit code
exit $TEST_EXIT_CODE
