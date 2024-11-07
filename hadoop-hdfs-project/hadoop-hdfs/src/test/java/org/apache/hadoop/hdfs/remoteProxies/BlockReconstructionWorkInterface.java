package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockReconstructionWorkInterface {
    byte getStoragePolicyID();
    void setNotEnoughRack();
    DatanodeStorageInfoInterface[] getTargets();
    BlockInfoInterface getBlock();
    int getAdditionalReplRequired();
    void resetTargets();
    long getBlockSize();
    void setTargets(DatanodeStorageInfoInterface[] arg0);
    java.lang.String getSrcPath();
    int getPriority();
    void addTaskToDatanode(NumberReplicasInterface arg0);
    void chooseTargets(BlockPlacementPolicyInterface arg0, BlockStoragePolicySuiteInterface arg1, java.util.Set<org.apache.hadoop.net.Node> arg2);
    DatanodeDescriptorInterface[] getSrcNodes();
    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> getContainingNodes();
    boolean hasNotEnoughRack();
    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> getLiveReplicaStorages();
}