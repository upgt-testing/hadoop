package org.apache.hadoop.hdfs.remoteProxies;

public interface INodeDirectoryProxy {
    boolean isWithSnapshot();
    boolean isSnapshottable();
}
