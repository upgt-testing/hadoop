package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.protocol.BlockJVMInterface;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfoJVMInterface;
import org.apache.hadoop.hdfs.server.namenode.snapshot.FileDiffListJVMInterface;

public interface INodeFileJVMInterface {
    BlockInfoJVMInterface[] getBlocks();
    boolean isUnderConstruction();
    short getFileReplication();
    int numBlocks();
    BlockInfoJVMInterface getLastBlock();
    FileDiffListJVMInterface getDiffs();
}
