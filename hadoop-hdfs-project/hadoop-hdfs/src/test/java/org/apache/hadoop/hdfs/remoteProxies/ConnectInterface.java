package org.apache.hadoop.hdfs.remoteProxies;

public interface ConnectInterface {
    void failed(java.lang.Throwable arg0);
    void run();
    java.lang.String toString();
    void update(java.nio.channels.Selector arg0);
}