package org.apache.hadoop.hdfs.remoteProxies;

public interface NewShmInfoInterface {
    java.io.FileInputStream getFileStream();
    ShmIdInterface getShmId();
    void close() throws java.io.IOException;
}