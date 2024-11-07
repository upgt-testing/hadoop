package org.apache.hadoop.hdfs.remoteProxies;

public interface ByteArrayBuilderInterface {
    void write(int arg0);
    byte[] finishCurrentSegment();
    void release();
    byte[] getCurrentSegment();
    void appendThreeBytes(int arg0);
    int getCurrentSegmentLength();
    void _allocMore();
    byte[] completeAndCoalesce(int arg0);
    void write(byte[] arg0);
    void appendFourBytes(int arg0);
    int size();
    byte[] resetAndGetFirstSegment();
    ByteArrayBuilderInterface fromInitial(byte[] arg0, int arg1);
    byte[] toByteArray();
    void append(int arg0);
    void setCurrentSegmentLength(int arg0);
    java.io.OutputStream nullOutputStream();
    void write(byte[] arg0, int arg1, int arg2);
    void flush();
    void close();
    void reset();
    void appendTwoBytes(int arg0);
}