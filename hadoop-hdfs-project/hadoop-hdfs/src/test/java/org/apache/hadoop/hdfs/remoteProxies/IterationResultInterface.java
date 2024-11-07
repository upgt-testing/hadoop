package org.apache.hadoop.hdfs.remoteProxies;

public interface IterationResultInterface {
    java.util.Optional<org.apache.hadoop.hdfs.protocol.Block> getNextBlock();
    java.util.List<org.apache.hadoop.hdfs.server.common.FileRegion> getFileRegions();
}