#!/bin/bash

DEMO_ID=$1

# if DEMO_ID is not provided, then set it to 1
if [ -z "$DEMO_ID" ]; then
  DEMO_ID=1
fi

# if DEMO_ID is 1, then run the following commands, else run the other commands
if [ $DEMO_ID -eq 1 ]; then
    echo "Running demo TestCloseDocker"
    sleep 2
    mvn surefire:test -Dtest=TestCloseDocker
else
    echo "Running demo TestDFSInputStreamDocker#testSkipWithRemoteBlockReader"
    sleep 2
    mvn surefire:test -Dtest=TestDFSInputStreamDocker#testSkipWithRemoteBlockReader
fi
