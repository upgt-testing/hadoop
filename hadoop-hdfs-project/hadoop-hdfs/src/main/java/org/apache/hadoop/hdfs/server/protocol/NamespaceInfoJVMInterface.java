package org.apache.hadoop.hdfs.server.protocol;

public interface NamespaceInfoJVMInterface {
    String getBlockPoolID();
    int getNamespaceID();
    String getClusterID();
    long getCTime();
}
