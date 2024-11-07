package org.apache.hadoop.hdfs.remoteProxies;

public interface LongWritableInterface {
    boolean equals(java.lang.Object arg0);
    long get();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    int hashCode();
    int compareTo(LongWritableInterface arg0);
    java.lang.String toString();
    void set(long arg0);
}