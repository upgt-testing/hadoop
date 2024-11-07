package org.apache.hadoop.hdfs.remoteProxies;

public interface LeaseManagerInterface {
    void removeAllLeases();
    void removeLease(java.lang.String arg0, INodeFileInterface arg1);
    long countPath();
    void triggerMonitorCheckNow();
    boolean checkLeases();
    LeaseInterface getLease(java.lang.String arg0);
    void setLeasePeriod(long arg0, long arg1);
    int countLease();
    LeaseInterface reassignLease(LeaseInterface arg0, INodeFileInterface arg1, java.lang.String arg2);
//    java.util.Collection<org.apache.hadoop.hdfs.server.namenode.LeaseManager.Lease> getExpiredCandidateLeases();
    INodeInterface[] getINodesWithLease();
    void updateInternalLeaseHolder();
    void removeLease(LeaseInterface arg0, long arg1);
    java.lang.String toString();
    void startMonitor();
    void renewLease(java.lang.String arg0);
    BatchedListEntriesInterface<org.apache.hadoop.hdfs.protocol.OpenFileEntry> getUnderConstructionFiles(long arg0) throws java.io.IOException;
    BatchedListEntriesInterface<org.apache.hadoop.hdfs.protocol.OpenFileEntry> getUnderConstructionFiles(long arg0, java.lang.String arg1) throws java.io.IOException;
    LeaseInterface getLease(INodeFileInterface arg0);
    java.util.Set<org.apache.hadoop.hdfs.server.namenode.INodesInPath> getINodeWithLeases(INodeDirectoryInterface arg0) throws java.io.IOException;
    void renewAllLeases();
    void runLeaseChecks();
//    boolean checkLeases(java.util.Collection<org.apache.hadoop.hdfs.server.namenode.LeaseManager.Lease> arg0);
    boolean isMaxLockHoldToReleaseLease(long arg0);
    void removeLease(long arg0);
    void renewLease(LeaseInterface arg0);
    java.util.Collection<java.lang.Long> getINodeIdWithLeases();
    java.lang.String getInternalLeaseHolder();
    void stopMonitor();
    java.util.Set<org.apache.hadoop.hdfs.server.namenode.INodesInPath> getINodeWithLeases() throws java.io.IOException;
    LeaseInterface addLease(java.lang.String arg0, long arg1);
    long getNumUnderConstructionBlocks();
}