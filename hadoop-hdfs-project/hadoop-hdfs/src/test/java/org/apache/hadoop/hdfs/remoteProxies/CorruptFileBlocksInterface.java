package org.apache.hadoop.hdfs.remoteProxies;

public interface CorruptFileBlocksInterface {
    int hashCode();
    boolean equals(java.lang.Object arg0);
    java.lang.String[] getFiles();
    java.lang.String getCookie();
}