package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockUnderConstructionFeatureInterface {
    long getBlockRecoveryId();
    void setBlockUCState(org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState arg0);
//    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.ReplicaUnderConstruction> getStaleReplicas(long arg0);
    void updateStorageScheduledSize(BlockInfoStripedInterface arg0);
    byte[] getBlockIndices();
    DatanodeStorageInfoInterface[] getExpectedStorageLocations();
    java.util.Iterator<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> getExpectedStorageLocationsIterator();
    void appendUCParts(java.lang.StringBuilder arg0);
    void setExpectedLocations(BlockInterface arg0, DatanodeStorageInfoInterface[] arg1, org.apache.hadoop.hdfs.protocol.BlockType arg2);
    void appendUCPartsConcise(java.lang.StringBuilder arg0);
    java.lang.String toString();
    void setTruncateBlock(BlockInfoInterface arg0);
    BlockInfoInterface getTruncateBlock();
    int getNumExpectedLocations();
    void initializeBlockRecovery(BlockInfoInterface arg0, long arg1, boolean arg2);
    void addReplicaIfNotPresent(DatanodeStorageInfoInterface arg0, BlockInterface arg1, org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState arg2);
    org.apache.hadoop.hdfs.server.common.HdfsServerConstants.BlockUCState getBlockUCState();
    void commit();
}