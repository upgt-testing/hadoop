#!/bin/bash

# Read the list of file names from test_list.txt
input_file=$1

# Check if the input file exists
if [[ ! -f "$input_file" ]]; then
  echo "Error: File '$input_file' not found!"
  exit 1
fi

mkdir -p logs

# Loop through each file name in the list
while IFS= read -r base_name; do
    echo mvn -B surefire:test -Dtest=${base_name} -Dupgt.datanode.restart -Dupgt.namenode.restart
    mvn -B surefire:test -Dtest=${base_name} -Dupgt.datanode.restart -Dupgt.namenode.restart > logs/${base_name}.log
done < "$input_file"

