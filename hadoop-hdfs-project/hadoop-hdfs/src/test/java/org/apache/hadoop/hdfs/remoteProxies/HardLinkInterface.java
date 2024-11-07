package org.apache.hadoop.hdfs.remoteProxies;

public interface HardLinkInterface {
    int getLinkCount(java.io.File arg0) throws java.io.IOException;
    void createHardLink(java.io.File arg0, java.io.File arg1) throws java.io.IOException;
    void createHardLinkMult(java.io.File arg0, java.lang.String[] arg1, java.io.File arg2) throws java.io.IOException;
    java.io.IOException createIOException(java.io.File arg0, java.lang.String arg1, java.lang.String arg2, int arg3, java.lang.Exception arg4);
}