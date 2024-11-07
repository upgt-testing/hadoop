package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockRecoveryWorkerInterface {
    ReplicaRecoveryInfoInterface callInitReplicaRecovery(org.apache.hadoop.hdfs.server.protocol.InterDatanodeProtocol arg0, RecoveringBlockInterface arg1) throws java.io.IOException;
    DaemonInterface recoverBlocks(java.lang.String arg0, java.util.Collection<org.apache.hadoop.hdfs.server.protocol.BlockRecoveryCommand.RecoveringBlock> arg1);
    DatanodeIDInterface getDatanodeID(java.lang.String arg0) throws java.io.IOException;
    void logRecoverBlock(java.lang.String arg0, RecoveringBlockInterface arg1);
    DatanodeProtocolClientSideTranslatorPBInterface getActiveNamenodeForBP(java.lang.String arg0) throws java.io.IOException;
}