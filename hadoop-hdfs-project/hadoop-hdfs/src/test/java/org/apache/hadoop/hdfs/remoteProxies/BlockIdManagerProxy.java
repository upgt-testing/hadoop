package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockIdManagerProxy {
    SequentialBlockIdGeneratorProxy getBlockIdGenerator();
    SequentialBlockGroupIdGeneratorProxy getBlockGroupIdGenerator();
}
