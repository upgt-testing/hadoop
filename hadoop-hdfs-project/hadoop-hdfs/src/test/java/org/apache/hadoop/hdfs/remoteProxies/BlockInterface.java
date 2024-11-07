package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockInterface {
    void setGenerationStamp(long arg0);
    boolean equals(java.lang.Object arg0);
    int hashCode();
    void readId(java.io.DataInput arg0) throws java.io.IOException;
    void writeHelper(java.io.DataOutput arg0) throws java.io.IOException;
    boolean matchingIdAndGenStamp(BlockInterface arg0, BlockInterface arg1);
    void writeId(java.io.DataOutput arg0) throws java.io.IOException;
    int compareTo(BlockInterface arg0);
    java.lang.String toString(BlockInterface arg0);
    void setNumBytes(long arg0);
    long getBlockId(java.lang.String arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void readHelper(java.io.DataInput arg0) throws java.io.IOException;
    long filename2id(java.lang.String arg0);
    long getBlockId();
    void setBlockId(long arg0);
    void set(long arg0, long arg1, long arg2);
    long getGenerationStamp();
    java.lang.String getBlockName();
    long getGenerationStamp(java.lang.String arg0);
    java.lang.String toString();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    boolean isBlockFilename(java.io.File arg0);
    long getNumBytes();
    boolean isMetaFilename(java.lang.String arg0);
    void appendStringTo(java.lang.StringBuilder arg0);
    java.io.File metaToBlockFile(java.io.File arg0);
}