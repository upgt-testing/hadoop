package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.hdfs.security.token.block.BlockTokenSecretManagerJVMInterface;

public interface BlockManagerJVMInterface {
    BlockTokenSecretManagerJVMInterface getBlockTokenSecretManager();
}
