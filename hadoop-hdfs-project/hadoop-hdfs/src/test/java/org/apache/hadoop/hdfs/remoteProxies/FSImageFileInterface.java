package org.apache.hadoop.hdfs.remoteProxies;

public interface FSImageFileInterface {
    java.lang.String toString();
    long getCheckpointTxId();
    java.io.File getFile();
}