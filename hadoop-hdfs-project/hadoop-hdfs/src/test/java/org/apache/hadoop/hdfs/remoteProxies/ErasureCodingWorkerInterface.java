package org.apache.hadoop.hdfs.remoteProxies;

public interface ErasureCodingWorkerInterface {
    ConfigurationInterface getConf();
    void processErasureCodingTasks(java.util.Collection<org.apache.hadoop.hdfs.server.protocol.BlockECReconstructionCommand.BlockECReconstructionInfo> arg0);
    DataNodeInterface getDatanode();
    void initializeStripedBlkReconstructionThreadPool(int arg0);
    java.util.concurrent.CompletionService<org.apache.hadoop.hdfs.util.StripedBlockUtil.BlockReadStats> createReadService();
    void initializeStripedReadThreadPool();
    void shutDown();
    float getXmitWeight();
}