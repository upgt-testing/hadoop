package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeAdminManagerInterface {
    void stopMaintenance(DatanodeDescriptorInterface arg0);
    boolean isSufficient(BlockInfoInterface arg0, org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg1, NumberReplicasInterface arg2, boolean arg3, boolean arg4);
    void setDecommissioned(DatanodeDescriptorInterface arg0);
    java.util.Queue<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> getPendingNodes();
    void activate(ConfigurationInterface arg0);
    int getNumNodesChecked();
    int getNumTrackedNodes();
    void close();
    void stopDecommission(DatanodeDescriptorInterface arg0);
    void startDecommission(DatanodeDescriptorInterface arg0);
    int getNumPendingNodes();
    void startMaintenance(DatanodeDescriptorInterface arg0, long arg1);
    void setInMaintenance(DatanodeDescriptorInterface arg0);
    void logBlockReplicationInfo(BlockInfoInterface arg0, org.apache.hadoop.hdfs.server.blockmanagement.BlockCollection arg1, DatanodeDescriptorInterface arg2, NumberReplicasInterface arg3, java.lang.Iterable<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg4);
    void runMonitorForTest() throws java.util.concurrent.ExecutionException, java.lang.InterruptedException;
}