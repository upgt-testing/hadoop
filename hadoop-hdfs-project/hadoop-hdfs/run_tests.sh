#!/bin/bash

# Read the list of file names from test_list.txt
input_file=$1

# Check if the input file exists
if [[ ! -f "$input_file" ]]; then
  echo "Error: File '$input_file' not found!"
  exit 1
fi

mkdir -p logs

# get the total number of test cases
total_tests=$(wc -l < $input_file)
counter=0
start_time=$(date)

# Loop through each file name in the list
while IFS= read -r base_name; do
    # print the progress
    echo "Running test $base_name"
    echo "Progress: $((++counter))/$total_tests"
    echo "Time elapsed: $(($(date +%s) - $(date -d "$start_time" +%s))) seconds"
    
    echo mvn -B surefire:test -Dtest=${base_name} -Dupgt.namenode.upgrade -Dupgt.datanode.upgrade -Dupgt.start.version=3.4.0 -Dupgt.upgrade.version=3.5.0-SNAPSHOT
    # restart ONLY
    #    mvn -B surefire:test -Dtest=${base_name} -Dupgt.datanode.restart -Dupgt.namenode.restart > logs/${base_name}.log

    # upgrade only
    mvn -B surefire:test -Dtest=${base_name} -Dupgt.namenode.upgrade -Dupgt.datanode.upgrade -Dupgt.start.version=3.4.0 -Dupgt.upgrade.version=3.5.0-SNAPSHOT > logs/${base_name}.log 2>&1
    
    # restart and upgrade
    #    mvn -B surefire:test -Dtest=${base_name} -Dupgt.namenode.restart -Dupgt.datanode.restart -Dupgt.namenode.upgrade -Dupgt.datanode.upgrade -Dupgt.start.version=3.4.0 -Dupgt.upgrade.version=3.5.0-SNAPSHOT > logs/${base_name}.log
    
done < "$input_file"

