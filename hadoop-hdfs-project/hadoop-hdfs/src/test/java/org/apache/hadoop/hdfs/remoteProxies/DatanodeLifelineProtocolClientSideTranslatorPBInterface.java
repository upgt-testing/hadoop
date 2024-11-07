package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeLifelineProtocolClientSideTranslatorPBInterface {
    org.apache.hadoop.hdfs.protocolPB.DatanodeLifelineProtocolPB createNamenode(java.net.InetSocketAddress arg0, ConfigurationInterface arg1, UserGroupInformationInterface arg2) throws java.io.IOException;
    void sendLifeline(DatanodeRegistrationInterface arg0, org.apache.hadoop.hdfs.server.protocol.StorageReport[] arg1, long arg2, long arg3, int arg4, int arg5, int arg6, VolumeFailureSummaryInterface arg7) throws java.io.IOException;
    boolean isMethodSupported(java.lang.String arg0) throws java.io.IOException;
    void close() throws java.io.IOException;
}