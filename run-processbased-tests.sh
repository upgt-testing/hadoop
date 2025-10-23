#!/bin/bash

################################################################################
# Script to run all ProcessBased tests with ProcessBasedMiniDFSCluster
# This script runs tests using the same Hadoop version for both start and upgrade
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
HADOOP_VERSION="3.3.5"
HADOOP_DIST_DIR="/tmp/hadoop-test-distributions"
HADOOP_HOME="${HADOOP_DIST_DIR}/hadoop-${HADOOP_VERSION}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RESULTS_DIR="${PROJECT_ROOT}/test-results-processbased-${TIMESTAMP}"

# Test configuration
SKIP_DOWNLOAD=false
SKIP_BUILD=false
TEST_FILTER=""

# Function to print colored output
print_header() {
    echo -e "${BLUE}========================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Function to show help
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Run all ProcessBased tests METHOD BY METHOD using ProcessBasedMiniDFSCluster framework.
Each test method runs in complete isolation for better debugging and tracking.

OPTIONS:
    --skip-download     Skip downloading Hadoop distribution (use existing)
    --skip-build       Skip building Hadoop project
    --test-filter      Run only tests matching pattern (e.g., "*Balancer*")
    --clean            Clean up downloaded distributions and build artifacts
    --help             Show this help message

EXAMPLES:
    # Run all ProcessBased test methods (downloads distribution if needed)
    $0

    # Run tests, skip download (if distribution already exists)
    $0 --skip-download

    # Run only specific test class methods
    $0 --test-filter "*Balancer*"

    # Clean up everything
    $0 --clean

NOTES:
    - Tests run METHOD BY METHOD for complete isolation
    - Each method execution is logged separately
    - Progress is tracked in real-time
    - Both hadoop.start.home and hadoop.upgrade.home point to the same version

EOF
}

# Parse command line arguments
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
        --test-filter)
            TEST_FILTER="$2"
            shift 2
            ;;
        --clean)
            print_header "Cleaning up test artifacts"
            print_info "Removing downloaded distributions..."
            rm -rf "${HADOOP_DIST_DIR}"
            print_info "Cleaning Maven build..."
            mvn clean -pl hadoop-hdfs-project/hadoop-hdfs
            rm -rf test-results-processbased-*
            print_success "Cleanup complete"
            exit 0
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Function to download Hadoop distribution
download_hadoop() {
    local version=$1
    local dist_dir=$2
    local hadoop_dir="${dist_dir}/hadoop-${version}"
    local archive="${hadoop_dir}.tar.gz"

    if [ -d "$hadoop_dir" ]; then
        print_success "Hadoop ${version} already exists at ${hadoop_dir}"
        return 0
    fi

    print_info "Downloading Hadoop ${version}..."
    mkdir -p "$dist_dir"
    cd "$dist_dir"

    local url="https://archive.apache.org/dist/hadoop/common/hadoop-${version}/hadoop-${version}.tar.gz"

    if ! wget -q --show-progress "$url"; then
        print_error "Failed to download Hadoop ${version}"
        return 1
    fi

    print_info "Extracting Hadoop ${version}..."
    if ! tar xzf "hadoop-${version}.tar.gz"; then
        print_error "Failed to extract Hadoop ${version}"
        return 1
    fi

    rm -f "hadoop-${version}.tar.gz"
    print_success "Hadoop ${version} ready at ${hadoop_dir}"
    cd "$PROJECT_ROOT"
}

# Function to build Hadoop project
build_hadoop() {
    print_info "Building Hadoop project (this may take several minutes)..."

    if ! mvn install -pl hadoop-hdfs-project/hadoop-hdfs -am -DskipTests -q; then
        print_error "Build failed"
        return 1
    fi

    print_success "Build completed successfully"
}

# Function to get list of ProcessBased test classes
get_processbased_test_classes() {
    local tests=(
        "org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeHdfsFileSystemContract_ProcessBased"
        "org.apache.hadoop.fs.viewfs.TestViewFileSystemOverloadSchemeWithHdfsScheme_ProcessBased"
        "org.apache.hadoop.fs.viewfs.TestViewFsDefaultValue_ProcessBased"
        "org.apache.hadoop.hdfs.TestClientProtocolForPipelineRecovery_ProcessBased"
        "org.apache.hadoop.hdfs.TestDistributedFileSystem_ProcessBased"
        "org.apache.hadoop.hdfs.TestFileAppend2_ProcessBased"
        "org.apache.hadoop.hdfs.TestFileAppendRestart_ProcessBased"
        "org.apache.hadoop.hdfs.TestFileAppend_ProcessBased"
        "org.apache.hadoop.hdfs.TestPread_ProcessBased"
        "org.apache.hadoop.hdfs.TestReplaceDatanodeOnFailure_ProcessBased"
        "org.apache.hadoop.hdfs.server.balancer.TestBalancerLongRunningTasks_ProcessBased"
        "org.apache.hadoop.hdfs.server.balancer.TestBalancer_ProcessBased"
        "org.apache.hadoop.hdfs.server.mover.TestMover_ProcessBased"
        "org.apache.hadoop.hdfs.server.namenode.TestINodeFile_ProcessBased"
        "org.apache.hadoop.hdfs.server.namenode.TestStripedINodeFile_ProcessBased"
        "org.apache.hadoop.hdfs.server.namenode.snapshot.TestSnapshotFileLength_ProcessBased"
        "org.apache.hadoop.hdfs.server.namenode.sps.TestStoragePolicySatisfierWithStripedFile_ProcessBased"
        "org.apache.hadoop.hdfs.server.sps.TestExternalStoragePolicySatisfier_ProcessBased"
        "org.apache.hadoop.hdfs.tools.TestDFSAdmin_ProcessBased"
    )

    if [ -n "$TEST_FILTER" ]; then
        local filtered=()
        for test in "${tests[@]}"; do
            if [[ $test == $TEST_FILTER ]]; then
                filtered+=("$test")
            fi
        done
        echo "${filtered[@]}"
    else
        echo "${tests[@]}"
    fi
}

# Function to convert package name to file path
package_to_path() {
    local package_name=$1
    local class_name=$(echo "$package_name" | awk -F'.' '{print $NF}')
    local package_path=$(echo "$package_name" | sed 's/\./\//g')
    echo "${PROJECT_ROOT}/hadoop-hdfs-project/hadoop-hdfs/src/test/java/${package_path}.java"
}

# Function to extract test methods from a test class
get_test_methods() {
    local class_name=$1
    local file_path=$(package_to_path "$class_name")

    if [ ! -f "$file_path" ]; then
        print_error "Test file not found: $file_path"
        return 1
    fi

    # Extract method names that are preceded by @Test annotation
    # This handles both @Test and @Test(timeout=...) patterns
    local methods=()
    local in_test=false

    while IFS= read -r line; do
        if [[ $line =~ @Test ]]; then
            in_test=true
        elif [ "$in_test" = true ] && [[ $line =~ public[[:space:]]+void[[:space:]]+([a-zA-Z0-9_]+)[[:space:]]*\( ]]; then
            local method_name="${BASH_REMATCH[1]}"
            methods+=("$method_name")
            in_test=false
        fi
    done < "$file_path"

    echo "${methods[@]}"
}

# Function to get all test methods from all classes
get_all_test_methods() {
    local classes=($(get_processbased_test_classes))
    local all_methods=()

    for class in "${classes[@]}"; do
        local methods=($(get_test_methods "$class"))
        for method in "${methods[@]}"; do
            all_methods+=("${class}#${method}")
        done
    done

    echo "${all_methods[@]}"
}

# Function to run tests method by method
run_tests() {
    print_info "Discovering test methods..."
    local test_methods=($(get_all_test_methods))
    local total_methods=${#test_methods[@]}

    print_info "Found ${total_methods} test methods across all ProcessBased test classes"

    mkdir -p "$RESULTS_DIR"
    mkdir -p "${RESULTS_DIR}/surefire-reports"

    # Create a progress tracking file
    local progress_file="${RESULTS_DIR}/progress.txt"
    echo "0/${total_methods}" > "$progress_file"

    local passed=0
    local failed=0
    local errors=0
    local skipped=0
    local count=0

    # Run each test method individually
    for test_method in "${test_methods[@]}"; do
        count=$((count + 1))

        local class_name="${test_method%#*}"
        local method_name="${test_method#*#}"
        local short_class="${class_name##*.}"

        echo ""
        print_info "[${count}/${total_methods}] Running: ${short_class}#${method_name}"

        # Run Maven test for this specific method
        local maven_cmd="mvn test -pl hadoop-hdfs-project/hadoop-hdfs"
        maven_cmd+=" -Dtest=${class_name}#${method_name}"
        maven_cmd+=" -Dhadoop.start.home=${HADOOP_HOME}"
        maven_cmd+=" -Dhadoop.upgrade.home=${HADOOP_HOME}"

        # Run the test and capture result
        local test_log="${RESULTS_DIR}/test-${count}-${short_class}-${method_name}.log"
        if ${maven_cmd} > "$test_log" 2>&1; then
            print_success "PASSED: ${short_class}#${method_name}"
            passed=$((passed + 1))
        else
            # Check if it was a failure or error
            if grep -q "Failures: 0" "$test_log" && grep -q "Errors: 0" "$test_log"; then
                # Test was skipped
                print_warning "SKIPPED: ${short_class}#${method_name}"
                skipped=$((skipped + 1))
            elif grep -q "Failures: [1-9]" "$test_log"; then
                print_error "FAILED: ${short_class}#${method_name}"
                failed=$((failed + 1))
            else
                print_error "ERROR: ${short_class}#${method_name}"
                errors=$((errors + 1))
            fi
        fi

        # Update progress
        echo "${count}/${total_methods} (Passed: ${passed}, Failed: ${failed}, Errors: ${errors}, Skipped: ${skipped})" > "$progress_file"

        # Copy surefire reports for this test
        if [ -d "hadoop-hdfs-project/hadoop-hdfs/target/surefire-reports" ]; then
            cp hadoop-hdfs-project/hadoop-hdfs/target/surefire-reports/* "${RESULTS_DIR}/surefire-reports/" 2>/dev/null || true
        fi
    done

    # Create summary log
    cat >> "${RESULTS_DIR}/test-execution.log" << EOF

========================================================================
Test Execution Summary
========================================================================
Total Methods:  ${total_methods}
Passed:         ${passed}
Failed:         ${failed}
Errors:         ${errors}
Skipped:        ${skipped}
========================================================================

EOF

    print_success "All test methods completed"
    print_info "Results: ${passed} passed, ${failed} failed, ${errors} errors, ${skipped} skipped"
}

# Function to generate summary
generate_summary() {
    local summary_file="${RESULTS_DIR}/SUMMARY.txt"

    cat > "$summary_file" << EOF
=======================================================================
ProcessBased Tests Summary
=======================================================================

Test Execution Date: $(date)
Hadoop Version:      ${HADOOP_VERSION}
Hadoop Home:         ${HADOOP_HOME}

Environment:
  hadoop.start.home:   ${HADOOP_HOME}
  hadoop.upgrade.home: ${HADOOP_HOME}

Results Directory: ${RESULTS_DIR}

EOF

    # Parse test results from surefire reports
    if [ -d "${RESULTS_DIR}/surefire-reports" ]; then
        local tests_run=$(grep -h "Tests run:" "${RESULTS_DIR}/surefire-reports"/*.txt 2>/dev/null | \
                         awk -F'Tests run: ' '{print $2}' | awk -F',' '{sum+=$1} END {print sum}')
        local failures=$(grep -h "Failures:" "${RESULTS_DIR}/surefire-reports"/*.txt 2>/dev/null | \
                        awk -F'Failures: ' '{print $2}' | awk -F',' '{sum+=$1} END {print sum}')
        local errors=$(grep -h "Errors:" "${RESULTS_DIR}/surefire-reports"/*.txt 2>/dev/null | \
                      awk -F'Errors: ' '{print $2}' | awk -F',' '{sum+=$1} END {print sum}')
        local skipped=$(grep -h "Skipped:" "${RESULTS_DIR}/surefire-reports"/*.txt 2>/dev/null | \
                       awk -F'Skipped: ' '{print $2}' | awk -F',' '{sum+=$1} END {print sum}')

        cat >> "$summary_file" << EOF
Test Results:
  Tests run:     ${tests_run:-0}
  Failures:      ${failures:-0}
  Errors:        ${errors:-0}
  Skipped:       ${skipped:-0}

EOF

        if [ "${failures:-0}" -eq 0 ] && [ "${errors:-0}" -eq 0 ]; then
            echo "STATUS: SUCCESS ✓" >> "$summary_file"
        else
            echo "STATUS: FAILED ✗" >> "$summary_file"
        fi
    fi

    cat "$summary_file"
    print_success "Summary saved to ${summary_file}"
}

################################################################################
# Main execution
################################################################################

main() {
    print_header "ProcessBased Tests - Method by Method Execution"

    echo ""
    print_info "Configuration:"
    echo "  Hadoop Version: ${HADOOP_VERSION}"
    echo "  Distribution Directory: ${HADOOP_DIST_DIR}"
    echo "  Results Directory: ${RESULTS_DIR}"
    echo "  Skip Download: ${SKIP_DOWNLOAD}"
    echo "  Skip Build: ${SKIP_BUILD}"
    echo "  Execution Mode: METHOD BY METHOD (complete isolation)"
    if [ -n "$TEST_FILTER" ]; then
        echo "  Test Filter: ${TEST_FILTER}"
    fi
    echo ""

    # Step 1: Download Hadoop distribution
    if [ "$SKIP_DOWNLOAD" = false ]; then
        print_header "Step 1: Downloading Hadoop Distribution"
        if ! download_hadoop "$HADOOP_VERSION" "$HADOOP_DIST_DIR"; then
            print_error "Failed to download Hadoop distribution"
            exit 1
        fi
    else
        print_header "Step 1: Skipping Download (using existing distribution)"
        if [ ! -d "$HADOOP_HOME" ]; then
            print_error "Hadoop distribution not found at ${HADOOP_HOME}"
            print_info "Run without --skip-download to download it automatically"
            exit 1
        fi
        print_success "Using existing Hadoop at ${HADOOP_HOME}"
    fi

    # Step 2: Build Hadoop project
    if [ "$SKIP_BUILD" = false ]; then
        print_header "Step 2: Building Hadoop Project"
        if ! build_hadoop; then
            print_error "Build failed"
            exit 1
        fi
    else
        print_header "Step 2: Skipping Build"
        print_warning "Using existing build artifacts"
    fi

    # Step 3: Run tests
    print_header "Step 3: Running ProcessBased Tests"
    run_tests

    # Step 4: Generate summary
    print_header "Step 4: Generating Summary"
    generate_summary

    # Final message
    echo ""
    print_header "Execution Complete"
    print_success "Results available at: ${RESULTS_DIR}"
    print_info "To analyze results, run: ./analyze-test-results.sh ${RESULTS_DIR}"
    echo ""
}

# Run main function
main
