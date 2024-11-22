package org.apache.hadoop.hdfs.server.namenode;

public interface FSImageInterface {
    int getLayoutVersion();
    int getNamespaceID();
    String getClusterID();
    String getBlockPoolID();
}
