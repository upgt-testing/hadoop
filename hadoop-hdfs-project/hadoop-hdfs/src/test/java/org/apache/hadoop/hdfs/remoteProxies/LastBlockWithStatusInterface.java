package org.apache.hadoop.hdfs.remoteProxies;

public interface LastBlockWithStatusInterface {
    org.apache.hadoop.hdfs.protocol.HdfsFileStatus getFileStatus();
    LocatedBlockInterface getLastBlock();
}