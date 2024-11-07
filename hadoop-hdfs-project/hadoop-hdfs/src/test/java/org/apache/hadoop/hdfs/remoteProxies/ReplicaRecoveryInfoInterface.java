package org.apache.hadoop.hdfs.remoteProxies;

public interface ReplicaRecoveryInfoInterface {
    boolean isMetaFilename(java.lang.String arg0);
    boolean equals(java.lang.Object arg0);
    int compareTo(BlockInterface arg0);
    long getBlockId(java.lang.String arg0);
    java.io.File metaToBlockFile(java.io.File arg0);
    long getNumBytes();
    void appendStringTo(java.lang.StringBuilder arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    boolean matchingIdAndGenStamp(BlockInterface arg0, BlockInterface arg1);
    void writeHelper(java.io.DataOutput arg0) throws java.io.IOException;
    void readHelper(java.io.DataInput arg0) throws java.io.IOException;
    void writeId(java.io.DataOutput arg0) throws java.io.IOException;
    java.lang.String toString();
    long getGenerationStamp(java.lang.String arg0);
    void set(long arg0, long arg1, long arg2);
    void setNumBytes(long arg0);
    void readId(java.io.DataInput arg0) throws java.io.IOException;
    long getGenerationStamp();
    boolean isBlockFilename(java.io.File arg0);
    org.apache.hadoop.hdfs.server.common.HdfsServerConstants.ReplicaState getOriginalReplicaState();
    java.lang.String getBlockName();
    long getBlockId();
    java.lang.String toString(BlockInterface arg0);
    int hashCode();
    void setBlockId(long arg0);
    void setGenerationStamp(long arg0);
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    long filename2id(java.lang.String arg0);
}