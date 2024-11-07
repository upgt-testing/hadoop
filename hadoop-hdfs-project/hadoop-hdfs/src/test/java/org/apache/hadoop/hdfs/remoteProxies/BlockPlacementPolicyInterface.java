package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockPlacementPolicyInterface {
    void setExcludeSlowNodesEnabled(boolean arg0);
    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> chooseReplicasToDelete(java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg0, java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg1, int arg2, java.util.List<org.apache.hadoop.fs.StorageType> arg3, DatanodeDescriptorInterface arg4, DatanodeDescriptorInterface arg5);
    DatanodeStorageInfoInterface[] chooseTarget(java.lang.String arg0, int arg1, org.apache.hadoop.net.Node arg2, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg3, boolean arg4, java.util.Set<org.apache.hadoop.net.Node> arg5, long arg6, BlockStoragePolicyInterface arg7, java.util.EnumSet<org.apache.hadoop.hdfs.AddBlockFlag> arg8);
    boolean isMovable(java.util.Collection<org.apache.hadoop.hdfs.protocol.DatanodeInfo> arg0, DatanodeInfoInterface arg1, DatanodeInfoInterface arg2);
    boolean getExcludeSlowNodesEnabled();
    void initialize(ConfigurationInterface arg0, org.apache.hadoop.hdfs.server.blockmanagement.FSClusterStats arg1, NetworkTopologyInterface arg2, Host2NodesMapInterface arg3);
    org.apache.hadoop.hdfs.server.blockmanagement.BlockPlacementStatus verifyBlockPlacement(DatanodeInfoInterface[] arg0, int arg1);
    void adjustSetsWithChosenReplica(java.util.Map<java.lang.String, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo>> arg0, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg1, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg2, DatanodeStorageInfoInterface arg3);
    java.lang.String getRack(DatanodeInfoInterface arg0);
    DatanodeStorageInfoInterface[] chooseTarget(java.lang.String arg0, int arg1, org.apache.hadoop.net.Node arg2, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo> arg3, boolean arg4, java.util.Set<org.apache.hadoop.net.Node> arg5, long arg6, BlockStoragePolicyInterface arg7, java.util.EnumSet<org.apache.hadoop.hdfs.AddBlockFlag> arg8, java.util.EnumMap<org.apache.hadoop.fs.StorageType, java.lang.Integer> arg9);
    <T> DatanodeInfoInterface getDatanodeInfo(T arg0);
    <T> void splitNodesWithRack(java.lang.Iterable<T> arg0, java.util.Collection<T> arg1, java.util.Map<java.lang.String, java.util.List<T>> arg2, java.util.List<T> arg3, java.util.List<T> arg4);
    DatanodeStorageInfoInterface[] chooseTarget(java.lang.String arg0, int arg1, org.apache.hadoop.net.Node arg2, java.util.Set<org.apache.hadoop.net.Node> arg3, long arg4, java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor> arg5, BlockStoragePolicyInterface arg6, java.util.EnumSet<org.apache.hadoop.hdfs.AddBlockFlag> arg7);
}