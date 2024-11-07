package org.apache.hadoop.hdfs.remoteProxies;

public interface INodeMapInterface {
    int size();
    INodeInterface get(long arg0);
    INodeMapInterface newInstance(INodeDirectoryInterface arg0);
    java.util.Iterator<org.apache.hadoop.hdfs.server.namenode.INodeWithAdditionalFields> getMapIterator();
    void clear();
    void put(INodeInterface arg0);
    void remove(INodeInterface arg0);
}