package org.apache.hadoop.hdfs.remoteProxies;

public interface LocatedBlockBuilderInterface {
    LocatedBlocksInterface build(DatanodeDescriptorInterface arg0);
    LocatedBlockBuilderInterface encryption(FileEncryptionInfoInterface arg0);
    LocatedBlockBuilderInterface lastUC(boolean arg0);
    LocatedBlockBuilderInterface fileLength(long arg0);
    LocatedBlockBuilderInterface erasureCoding(ErasureCodingPolicyInterface arg0);
    LocatedBlockBuilderInterface lastBlock(LocatedBlockInterface arg0);
    boolean isBlockMax();
    LocatedBlocksInterface build();
    LocatedBlockBuilderInterface addBlock(LocatedBlockInterface arg0);
    LocatedBlockBuilderInterface lastComplete(boolean arg0);
    LocatedBlockInterface newLocatedBlock(ExtendedBlockInterface arg0, DatanodeStorageInfoInterface[] arg1, long arg2, boolean arg3);
}