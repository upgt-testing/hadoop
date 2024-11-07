package org.apache.hadoop.hdfs.remoteProxies;

public interface DomainPeerServerInterface {
    java.lang.String getBindPath();
    java.lang.String toString();
    void setReceiveBufferSize(int arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
    org.apache.hadoop.hdfs.net.Peer accept() throws java.io.IOException, java.net.SocketTimeoutException;
    java.lang.String getListeningString();
    int getReceiveBufferSize() throws java.io.IOException;
}