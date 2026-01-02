#!/bin/bash
# Script to copy pre-built Hadoop jars to lib/ directory
# Run this after building hadoop-hdfs and hadoop-common

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"

HADOOP_HDFS_DIR="$SCRIPT_DIR/../hadoop-hdfs/target"
HADOOP_COMMON_DIR="$SCRIPT_DIR/../../../hadoop-common-project/hadoop-common/target"

echo "Creating lib directory..."
mkdir -p "$LIB_DIR"

echo "Copying hadoop-hdfs jars..."
cp "$HADOOP_HDFS_DIR/hadoop-hdfs-3.3.5.jar" "$LIB_DIR/"
cp "$HADOOP_HDFS_DIR/hadoop-hdfs-3.3.5-tests.jar" "$LIB_DIR/"

echo "Copying hadoop-hdfs-client jar..."
HADOOP_HDFS_CLIENT_DIR="$SCRIPT_DIR/../hadoop-hdfs-client/target"
cp "$HADOOP_HDFS_CLIENT_DIR/hadoop-hdfs-client-3.3.5.jar" "$LIB_DIR/"

echo "Copying hadoop-common jars..."
cp "$HADOOP_COMMON_DIR/hadoop-common-3.3.5.jar" "$LIB_DIR/"
cp "$HADOOP_COMMON_DIR/hadoop-common-3.3.5-tests.jar" "$LIB_DIR/"

echo "Done! Pre-built jars copied to lib/"
ls -lh "$LIB_DIR/"
