#!/bin/bash

# create the network
# remove the network if it already exists
docker network rm hadoop
docker network create --driver bridge hadoop

# create the namenode container
echo "Creating namenode container"
nohup docker run --name namenode --network hadoop --network-alias namenode -p 50070:50070 -p 9000:9000 -p 9870:9870 hadoop:3.3.6 bash -c "hdfs namenode -format && hdfs namenode" | tee namenode.log

# sleep for 10 seconds to allow the namenode to start
sleep 10

# create the datanode container
echo "Creating datanode containers"
nohup docker run --name datanode1 --network hadoop --network-alias datanode1 hadoop:3.3.6 bash -c "hdfs datanode" | tee datanode1.log 
nohup docker run --name datanode2 --network hadoop --network-alias datanode2 hadoop:3.3.6 bash -c "hdfs datanode" | tee datanode2.log 
