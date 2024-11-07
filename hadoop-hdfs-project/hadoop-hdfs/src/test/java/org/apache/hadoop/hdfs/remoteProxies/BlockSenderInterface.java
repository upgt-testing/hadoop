package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockSenderInterface {
    void manageOsCache() throws java.io.IOException;
    void verifyChecksum(byte[] arg0, int arg1, int arg2, int arg3, int arg4) throws org.apache.hadoop.fs.ChecksumException;
    long getOffset();
    void readChecksum(byte[] arg0, int arg1, int arg2) throws java.io.IOException;
    boolean didSendEntireByteRange();
    void close() throws java.io.IOException;
    int numberOfChunks(long arg0);
    DataChecksumInterface getChecksum();
    boolean isLongRead();
    java.io.IOException ioeToSocketException(java.io.IOException arg0);
    long sendBlock(java.io.DataOutputStream arg0, java.io.OutputStream arg1, DataTransferThrottlerInterface arg2) throws java.io.IOException;
    int writePacketHeader(java.nio.ByteBuffer arg0, int arg1, int arg2);
    long doSendBlock(java.io.DataOutputStream arg0, java.io.OutputStream arg1, DataTransferThrottlerInterface arg2) throws java.io.IOException;
    ChunkChecksumInterface getPartialChunkChecksumForFinalized(FinalizedReplicaInterface arg0) throws java.io.IOException;
    int sendPacket(java.nio.ByteBuffer arg0, int arg1, java.io.OutputStream arg2, boolean arg3, DataTransferThrottlerInterface arg4) throws java.io.IOException;
    org.apache.hadoop.hdfs.server.datanode.Replica getReplica(ExtendedBlockInterface arg0, DataNodeInterface arg1) throws org.apache.hadoop.hdfs.server.datanode.ReplicaNotFoundException;
}