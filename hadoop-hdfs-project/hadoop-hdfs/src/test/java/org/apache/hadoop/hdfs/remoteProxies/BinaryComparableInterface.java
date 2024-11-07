package org.apache.hadoop.hdfs.remoteProxies;

public interface BinaryComparableInterface {
    int compareTo(byte[] arg0, int arg1, int arg2);
    int compareTo(BinaryComparableInterface arg0);
    byte[] getBytes();
    int getLength();
    int hashCode();
    boolean equals(java.lang.Object arg0);
}