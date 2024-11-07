package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeProtocolClientSideTranslatorPBInterface {
    NamespaceInfoInterface versionRequest() throws java.io.IOException;
    org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolPB createNamenode(java.net.InetSocketAddress arg0, ConfigurationInterface arg1, UserGroupInformationInterface arg2) throws java.io.IOException;
    HeartbeatResponseInterface sendHeartbeat(DatanodeRegistrationInterface arg0, org.apache.hadoop.hdfs.server.protocol.StorageReport[] arg1, long arg2, long arg3, int arg4, int arg5, int arg6, VolumeFailureSummaryInterface arg7, boolean arg8, SlowPeerReportsInterface arg9, SlowDiskReportsInterface arg10) throws java.io.IOException;
    DatanodeRegistrationInterface registerDatanode(DatanodeRegistrationInterface arg0) throws java.io.IOException;
    DatanodeCommandInterface blockReport(DatanodeRegistrationInterface arg0, java.lang.String arg1, org.apache.hadoop.hdfs.server.protocol.StorageBlockReport[] arg2, BlockReportContextInterface arg3) throws java.io.IOException;
    void commitBlockSynchronization(ExtendedBlockInterface arg0, long arg1, long arg2, boolean arg3, boolean arg4, DatanodeIDInterface[] arg5, java.lang.String[] arg6) throws java.io.IOException;
    void reportBadBlocks(org.apache.hadoop.hdfs.protocol.LocatedBlock[] arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
    void blockReceivedAndDeleted(DatanodeRegistrationInterface arg0, java.lang.String arg1, org.apache.hadoop.hdfs.server.protocol.StorageReceivedDeletedBlocks[] arg2) throws java.io.IOException;
    boolean isMethodSupported(java.lang.String arg0) throws java.io.IOException;
    DatanodeCommandInterface cacheReport(DatanodeRegistrationInterface arg0, java.lang.String arg1, java.util.List<java.lang.Long> arg2) throws java.io.IOException;
    void errorReport(DatanodeRegistrationInterface arg0, int arg1, java.lang.String arg2) throws java.io.IOException;
}