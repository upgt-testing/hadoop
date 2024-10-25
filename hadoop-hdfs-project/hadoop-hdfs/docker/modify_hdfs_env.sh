RMI_CONNECTION_PORT=$1
RMI_OBJECT_PORT=$2

# if $2 == 0, then it is a namenode, otherwise it is a datanode

echo 'export HADOOP_OPTS="$HADOOP_OPTS \
     -Djava.rmi.server.hostname=localhost \
     -Drmi.connection.port='${RMI_CONNECTION_PORT}' \
     -Drmi.object.port='${RMI_OBJECT_PORT}'"' >> /opt/hadoop/etc/hadoop/hadoop-env.sh
