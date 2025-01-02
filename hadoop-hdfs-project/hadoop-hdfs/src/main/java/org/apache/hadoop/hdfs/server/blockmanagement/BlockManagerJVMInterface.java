package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManagerJVMInterface;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface BlockManagerJVMInterface {
    BlockTokenSecretManagerJVMInterface getBlockTokenSecretManager();
    DatanodeManagerJVMInterface getDatanodeManager();
    int getUnderReplicatedNotMissingBlocks();
    void updateState();
}
