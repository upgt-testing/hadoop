package org.apache.hadoop.hdfs.remoteProxies;

public interface BlocksMapUpdateInfoInterface {
    java.util.List<org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo> getToDeleteList();
    void clear();
    java.util.List<org.apache.hadoop.hdfs.server.namenode.INode.BlocksMapUpdateInfo.UpdatedReplicationInfo> toUpdateReplicationInfo();
    void addUpdateReplicationFactor(BlockInfoInterface arg0, short arg1);
    void addDeleteBlock(BlockInfoInterface arg0);
}