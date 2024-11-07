package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockReportLeaseManagerInterface {
    long getNextId();
    long removeLease(DatanodeDescriptorInterface arg0);
    long requestLease(DatanodeDescriptorInterface arg0);
    void pruneExpiredPending(long arg0);
    boolean checkLease(DatanodeDescriptorInterface arg0, long arg1, long arg2);
    void register(DatanodeDescriptorInterface arg0);
    void unregister(DatanodeDescriptorInterface arg0);
    boolean pruneIfExpired(long arg0, NodeDataInterface arg1);
    NodeDataInterface registerNode(DatanodeDescriptorInterface arg0);
    void remove(NodeDataInterface arg0);
}