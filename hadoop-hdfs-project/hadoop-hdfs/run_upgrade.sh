#!/bin/bash
TEST_LIST_FILE=$1
START_VERSION=$2
END_VERSION=$3
SKIP_UPGRADE=$4

# check TEST_LIST_FILE, START_VERSION, END_VERSION and SKIP_UPGRADE are not empty
if [ -z "$TEST_LIST_FILE" ] || [ -z "$START_VERSION" ] || [ -z "$END_VERSION" ] || [ -z "$SKIP_UPGRADE" ]; then
	echo "Usage: $0 <TEST_LIST_FILE> <START_VERSION> <END_VERSION> <SKIP_UPGRADE>"
	exit 1
fi

LOGS_DIR="logs"
# if LOGS_DIR directory does not exist, create it
if [ ! -d "$LOGS_DIR" ]; then
    mkdir $LOGS_DIR
fi



for test in $(cat $TEST_LIST_FILE)
do
    # if SKIL_UPGRADE is not true or false, then quit with error
    if [ "$SKIP_UPGRADE" != "true" ] && [ "$SKIP_UPGRADE" != "false" ]; then
	echo "Invalid value for SKIL_UPGRADE. It should be either true or false"
	exit 1
    fi

    echo "mvn surefire:test -Dtest=${test} -DstartVersion=${START_VERSION} -DupgradeVersion=${END_VERSION} -Dupgradable.cluster.skip_upgrade=$SKIP_UPGRADE 2>&1 | tee $LOGS_DIR/${test}-$START_VERSION-$END_VERSION-$SKIP_UPGRADE.log"
    mvn surefire:test -Dtest=${test} -DstartVersion=${START_VERSION} -DupgradeVersion=${END_VERSION} -Dupgradable.cluster.skip_upgrade=$SKIP_UPGRADE 2>&1 | tee $LOGS_DIR/${test}-$START_VERSION-$END_VERSION-$SKIP_UPGRADE.log


done

