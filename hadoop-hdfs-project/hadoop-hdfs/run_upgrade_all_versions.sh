#!/bin/bash

#!/bin/bash
TEST_LIST_FILE=$1
SKIP_UPGRADE=$2

# List of versions
versions=(
  "hadoop:2.10.2"
  "hadoop:3.2.4"
  "hadoop:3.3.5"
  "hadoop:3.3.6"
  "hadoop:3.4.0"
)

echo "TEST_LIST_FILE=$TEST_LIST_FILE, SKIP_UPGRADE=$SKIP_UPGRADE"

# check TEST_LIST_FILE and SKIP_UPGRADE are not empty
if [ -z "$TEST_LIST_FILE" ] || [ -z "$SKIP_UPGRADE" ]; then
  echo "Usage: $0 <TEST_LIST_FILE> <SKIP_UPGRADE>"
  exit 1
fi

# Loop through the versions array
for ((i=0; i<${#versions[@]}-1; i++)); do
  START_VERSION=${versions[i]}
  END_VERSION=${versions[i+1]}
  
  echo "START_VERSION=$START_VERSION, END_VERSION=$END_VERSION"
  echo "bash run_upgrade.sh $TEST_LIST_FILE $START_VERSION $END_VERSION $SKIP_UPGRADE"
  bash run_upgrade.sh $TEST_LIST_FILE $START_VERSION $END_VERSION $SKIP_UPGRADE
  
done
