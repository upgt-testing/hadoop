PORT=$1
NODE_TYPE=$2

# if $2 == 0, then it is a namenode, otherwise it is a datanode
if [ $NODE_TYPE -eq 0 ]; then
    echo 'export HADOOP_NAMENODE_OPTS="$HADOOP_NAMENODE_OPTS \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port='${PORT}' \
    -Dcom.sun.management.jmxremote.rmi.port='${PORT}' \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=localhost"' >> /opt/hadoop/etc/hadoop/hadoop-env.sh

else
    echo 'export HADOOP_DATANODE_OPTS="$HADOOP_DATANODE_OPTS \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port='${PORT}' \
    -Dcom.sun.management.jmxremote.rmi.port='${PORT}' \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=localhost"' >> /opt/hadoop/etc/hadoop/hadoop-env.sh
fi

