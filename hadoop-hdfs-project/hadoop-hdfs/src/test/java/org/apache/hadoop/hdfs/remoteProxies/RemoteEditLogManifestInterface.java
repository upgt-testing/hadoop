package org.apache.hadoop.hdfs.remoteProxies;

public interface RemoteEditLogManifestInterface {
    java.util.List<org.apache.hadoop.hdfs.server.protocol.RemoteEditLog> getLogs();
    java.lang.String toString();
    long getCommittedTxnId();
    void checkState();
}