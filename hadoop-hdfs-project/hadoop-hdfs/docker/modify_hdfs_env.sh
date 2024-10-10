echo 'export HADOOP_NAMENODE_OPTS="$HADOOP_NAMENODE_OPTS \
-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=9090 \
-Dcom.sun.management.jmxremote.rmi.port=9090 \
-Dcom.sun.management.jmxremote.authenticate=false \
-Dcom.sun.management.jmxremote.ssl=false \
-Djava.rmi.server.hostname=localhost"' >> /opt/hadoop/etc/hadoop/hadoop-env.sh
