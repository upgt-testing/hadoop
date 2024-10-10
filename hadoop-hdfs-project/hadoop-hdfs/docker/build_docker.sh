#!/bin/bash
HADOOP_VERSION=$1

if [ -z "$HADOOP_VERSION" ]; then
  echo "Usage: $0 <HADOOP_VERSION>"
  exit 1
fi

cp ../target/hadoop-hdfs-${HADOOP_VERSION}.jar .
docker build --no-cache --build-arg HADOOP_BUILD_VERSION=$HADOOP_VERSION -t hadoop:$HADOOP_VERSION .
