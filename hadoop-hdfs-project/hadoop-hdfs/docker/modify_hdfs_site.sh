#!/bin/bash                                                                                                                                          

NODE_ID=$1
PORT1=$((50010 + $NODE_ID))
PORT2=$((50075 + $NODE_ID))
PORT3=$((50040 + $NODE_ID))

sed opt/hadoop/etc/hadoop/raw-hdfs-site.xml -e "s/50010/${PORT1}/g" -e "s/50075/${PORT2}/g" -e "s/50040/${PORT3}/g" > opt/hadoop/etc/hadoop/hdfs-site.xml
