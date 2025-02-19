package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfoJVMInterface;

public interface INodeFileJVMInterface {
    BlockInfoJVMInterface[] getBlocks();
    boolean isUnderConstruction();
}
