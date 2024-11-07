package org.apache.hadoop.hdfs.remoteProxies;

public interface ReplicaHandlerInterface {
    org.apache.hadoop.hdfs.server.datanode.ReplicaInPipeline getReplica();
    void close() throws java.io.IOException;
}