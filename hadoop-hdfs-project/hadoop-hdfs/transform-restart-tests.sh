#!/bin/bash
#
# Transform RestartInjected tests from one Hadoop version to another
#
# Usage: ./transform-restart-tests.sh <pattern-branch> <target-branch>
#
# Example: ./transform-restart-tests.sh hdfs-restart-3.3.5 hdfs-restart-3.2.4
#
# This script:
# 1. Extracts version from target-branch name
# 2. Lists all RestartInjected tests in pattern-branch
# 3. Checks which original tests exist in the target version
# 4. Outputs a summary for transformation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check arguments
if [ $# -ne 2 ]; then
    echo "Usage: $0 <pattern-branch> <target-branch>"
    echo ""
    echo "Example: $0 hdfs-restart-3.3.5 hdfs-restart-3.2.4"
    exit 1
fi

PATTERN_BRANCH="$1"
TARGET_BRANCH="$2"

# Extract version from target branch name
# Supports formats: hdfs-restart-3.2.4, restart-3.2.4, branch-3.2.4
extract_version() {
    echo "$1" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1
}

VERSION=$(extract_version "$TARGET_BRANCH")
if [ -z "$VERSION" ]; then
    echo -e "${RED}Error: Could not extract version from target branch '$TARGET_BRANCH'${NC}"
    echo "Expected format: hdfs-restart-X.Y.Z or similar"
    exit 1
fi

RELEASE_TAG="rel/release-$VERSION"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Restart Test Transformation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Pattern branch: ${GREEN}$PATTERN_BRANCH${NC}"
echo -e "Target branch:  ${GREEN}$TARGET_BRANCH${NC}"
echo -e "Target version: ${GREEN}$VERSION${NC}"
echo -e "Release tag:    ${GREEN}$RELEASE_TAG${NC}"
echo ""

# Verify branches/tags exist
echo -e "${YELLOW}Verifying branches and tags...${NC}"

if ! git rev-parse --verify "$PATTERN_BRANCH" > /dev/null 2>&1; then
    echo -e "${RED}Error: Pattern branch '$PATTERN_BRANCH' does not exist${NC}"
    exit 1
fi

if ! git rev-parse --verify "$RELEASE_TAG" > /dev/null 2>&1; then
    echo -e "${RED}Error: Release tag '$RELEASE_TAG' does not exist${NC}"
    echo "Available release tags:"
    git tag -l "rel/release-*" | head -10
    exit 1
fi

echo -e "${GREEN}Branches and tags verified.${NC}"
echo ""

# Find all RestartInjected tests in pattern branch
echo -e "${YELLOW}Finding RestartInjected tests in $PATTERN_BRANCH...${NC}"

RESTART_TESTS=$(git ls-tree -r --name-only "$PATTERN_BRANCH" | grep "_RestartInjected.java$" | sort)
TOTAL_TESTS=$(echo "$RESTART_TESTS" | grep -c "." || echo 0)

echo -e "Found ${GREEN}$TOTAL_TESTS${NC} RestartInjected tests"
echo ""

# Check which original tests exist in target version
echo -e "${YELLOW}Checking original test availability in $RELEASE_TAG...${NC}"
echo ""

CAN_TRANSFORM=0
WILL_SKIP=0
TRANSFORM_LIST=""
SKIP_LIST=""

# Project prefix for paths
PROJECT_PREFIX="hadoop-hdfs-project/hadoop-hdfs/"

while IFS= read -r restart_test; do
    [ -z "$restart_test" ] && continue

    # Extract base test name and path
    base_name=$(basename "$restart_test" _RestartInjected.java)
    dir_path=$(dirname "$restart_test")
    original_test="${dir_path}/${base_name}.java"

    # Build full path for release tag check
    full_original_path="${PROJECT_PREFIX}${original_test}"

    # Check if original exists in release tag
    if git show "${RELEASE_TAG}:${full_original_path}" > /dev/null 2>&1; then
        ((CAN_TRANSFORM++))
        TRANSFORM_LIST="${TRANSFORM_LIST}${restart_test}\n"
    else
        ((WILL_SKIP++))
        SKIP_LIST="${SKIP_LIST}  - ${base_name} (original not found in $VERSION)\n"
    fi
done <<< "$RESTART_TESTS"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Can transform:  ${GREEN}$CAN_TRANSFORM${NC} tests"
echo -e "Will skip:      ${YELLOW}$WILL_SKIP${NC} tests"
echo ""

if [ "$WILL_SKIP" -gt 0 ]; then
    echo -e "${YELLOW}Tests to skip (original doesn't exist in $VERSION):${NC}"
    echo -e "$SKIP_LIST"
fi

# Output transform list to file
OUTPUT_FILE="transform-list-${VERSION}.txt"
echo -e "$TRANSFORM_LIST" | grep -v "^$" > "$OUTPUT_FILE"

echo -e "${GREEN}Transform list written to: $OUTPUT_FILE${NC}"
echo ""

# Output commands for next steps
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Next Steps${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "1. Create/checkout target branch:"
echo -e "   ${YELLOW}git checkout $RELEASE_TAG && git checkout -b $TARGET_BRANCH${NC}"
echo ""
echo "2. Copy and adapt RestartInjectionFramework:"
echo -e "   ${YELLOW}git show ${PATTERN_BRANCH}:src/test/java/org/apache/hadoop/hdfs/RestartInjectionFramework.java${NC}"
echo ""
echo "3. For each test in $OUTPUT_FILE, transform by:"
echo "   a. Read pattern from: git show ${PATTERN_BRANCH}:<test_path>"
echo "   b. Read original from: git show ${RELEASE_TAG}:${PROJECT_PREFIX}<original_path>"
echo "   c. Apply restart injection pattern to original"
echo "   d. Write to target branch"
echo ""
echo "4. Compile and test:"
echo -e "   ${YELLOW}mvn test-compile -pl hadoop-hdfs-project/hadoop-hdfs -am${NC}"
echo ""
