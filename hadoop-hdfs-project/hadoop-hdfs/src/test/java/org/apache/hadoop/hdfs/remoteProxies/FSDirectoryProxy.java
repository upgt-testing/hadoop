package org.apache.hadoop.hdfs.remoteProxies;

public interface FSDirectoryProxy {
    long totalInodes();
    INodeProxy getINode(String path);
}
