#!/bin/bash

TEST_NAME=$1
bash copy_jars.sh
bash test-compile.sh ${TEST_NAME}.java
mvn surefire:test -Dtest=$TEST_NAME

