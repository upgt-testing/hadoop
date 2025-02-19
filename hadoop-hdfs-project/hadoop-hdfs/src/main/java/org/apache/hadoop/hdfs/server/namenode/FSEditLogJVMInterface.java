package org.apache.hadoop.hdfs.server.namenode;

public interface FSEditLogJVMInterface {
    long getLastWrittenTxId();
    long getCurSegmentTxId();
    void endCurrentLogSegment(boolean writeEndTxn);
    void restart();
    void setOutputBufferCapacity(int size);
}
