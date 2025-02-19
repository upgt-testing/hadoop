#!/bin/bash

# Read the list of file names from test_list.txt
input_file=$1
start_version=$2
upgrade_version=$3


# Check if the input file exists
if [[ ! -f "$input_file" ]]; then
	echo "File not found: $input_file"
	exit 1
fi
if [[ -z "$start_version" ]]; then
	echo "Please provide the start version"
	exit 1
fi
if [[ -z "$upgrade_version" ]]; then
	echo "Please provide the upgrade version"
	exit 1
fi

mkdir -p logs

# Loop through each file name in the list
while IFS= read -r base_name; do
    echo mvn -B surefire:test -Dtest=${base_name} -Dupgt.datanode.restart -Dupgt.namenode.restart
    # restart ONLY
    #    mvn -B surefire:test -Dtest=${base_name} -Dupgt.datanode.restart -Dupgt.namenode.restart > logs/${base_name}.log

    # upgrade only
    mvn -B surefire:test -Dtest=${base_name} -Dupgt.namenode.upgrade -Dupgt.datanode.upgrade -Dupgt.start.version=${start_version} -Dupgt.upgrade.version=${upgrade_version} > logs/${base_name}.log
    
    # restart and upgrade
    #    mvn -B surefire:test -Dtest=${base_name} -Dupgt.namenode.restart -Dupgt.datanode.restart -Dupgt.namenode.upgrade -Dupgt.datanode.upgrade -Dupgt.start.version=${start_version} -Dupgt.upgrade.version=${upgrade_version} > logs/${base_name}.log
    
done < "$input_file"

