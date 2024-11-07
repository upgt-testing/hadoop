package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockLocalPathInfoInterface {
    java.lang.String getBlockPath();
    ExtendedBlockInterface getBlock();
    java.lang.String getMetaPath();
    long getNumBytes();
}