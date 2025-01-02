package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManagerJVMInterface;

import java.util.List;

public interface BlockManagerJVMInterface {
    BlockTokenSecretManagerJVMInterface getBlockTokenSecretManager();
    DatanodeManagerJVMInterface getDatanodeManager();
}
