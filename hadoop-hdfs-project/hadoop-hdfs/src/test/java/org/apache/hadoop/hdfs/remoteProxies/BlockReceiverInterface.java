package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockReceiverInterface {
    java.util.zip.Checksum computePartialChunkCrc(long arg0, long arg1) throws java.io.IOException;
    boolean shouldVerifyChecksum();
    void adjustCrcFilePosition() throws java.io.IOException;
    void initPerfMonitoring(DatanodeInfoInterface[] arg0);
    void trackSendPacketToLastNodeInPipeline(long arg0);
    DataNodeInterface getDataNode();
    void handleMirrorOutError(java.io.IOException arg0) throws java.io.IOException;
    void manageWriterOsCache(long arg0);
    void close() throws java.io.IOException;
    void translateChunks(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg1);
    ReplicaHandlerInterface claimReplicaHandler();
    long checksum2long(byte[] arg0);
    org.apache.hadoop.hdfs.server.datanode.Replica getReplica();
    void flushOrSync(boolean arg0) throws java.io.IOException;
    java.lang.String getVolumeBaseUri();
    void cleanupBlock() throws java.io.IOException;
    byte[] copyLastChunkChecksum(byte[] arg0, int arg1, int arg2);
    void receiveBlock(java.io.DataOutputStream arg0, java.io.DataInputStream arg1, java.io.DataOutputStream arg2, java.lang.String arg3, DataTransferThrottlerInterface arg4, DatanodeInfoInterface[] arg5, boolean arg6) throws java.io.IOException;
    boolean packetSentInTime();
    void verifyChunks(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg1) throws java.io.IOException;
    int receivePacket() throws java.io.IOException;
    void sendOOB() throws java.io.IOException, java.lang.InterruptedException;
}