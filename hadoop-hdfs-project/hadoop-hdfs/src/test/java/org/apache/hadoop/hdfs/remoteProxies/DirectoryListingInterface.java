package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryListingInterface {
    byte[] getLastName();
    boolean hasMore();
    int getRemainingEntries();
    org.apache.hadoop.hdfs.protocol.HdfsFileStatus[] getPartialListing();
}