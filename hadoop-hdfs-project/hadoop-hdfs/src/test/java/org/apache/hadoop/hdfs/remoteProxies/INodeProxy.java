package org.apache.hadoop.hdfs.remoteProxies;

public interface INodeProxy {
    INodeDirectoryProxy asDirectory();
    boolean isDirectory();
}
