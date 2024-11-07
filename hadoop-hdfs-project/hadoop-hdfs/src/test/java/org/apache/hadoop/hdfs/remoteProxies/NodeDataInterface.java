package org.apache.hadoop.hdfs.remoteProxies;

public interface NodeDataInterface {
    NodeDataInterface ListHead(java.lang.String arg0);
    void removeSelf();
    void addToBeginning(NodeDataInterface arg0);
    void addToEnd(NodeDataInterface arg0);
}