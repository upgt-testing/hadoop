package org.apache.hadoop.hdfs.security.token.block;

public interface BlockPoolTokenSecretManagerJVMInterface {
    BlockTokenSecretManagerJVMInterface get(String bpid);
}
