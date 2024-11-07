package org.apache.hadoop.hdfs.remoteProxies;

public interface IncrementalBlockReportManagerInterface {
    void triggerIBR(boolean arg0);
    void sendIBRs(org.apache.hadoop.hdfs.server.protocol.DatanodeProtocol arg0, DatanodeRegistrationInterface arg1, java.lang.String arg2, java.lang.String arg3) throws java.io.IOException;
    void addRDBI(ReceivedDeletedBlockInfoInterface arg0, DatanodeStorageInterface arg1);
    int getPendingIBRSize();
    boolean sendImmediately();
    void triggerDeletionReportForTests();
    void putMissing(org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks[] arg0);
    PerStorageIBRInterface getPerStorageIBR(DatanodeStorageInterface arg0);
    void notifyNamenodeBlock(ReceivedDeletedBlockInfoInterface arg0, DatanodeStorageInterface arg1, boolean arg2);
    void waitTillNextIBR(long arg0);
    void clearIBRs();
    StorageReceivedDeletedBlocksInterface[] generateIBRs();
}