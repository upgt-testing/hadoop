package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryEntriesInterface {
    boolean hasMore();
    byte[] getToken();
    FileStatusInterface[] getEntries();
}