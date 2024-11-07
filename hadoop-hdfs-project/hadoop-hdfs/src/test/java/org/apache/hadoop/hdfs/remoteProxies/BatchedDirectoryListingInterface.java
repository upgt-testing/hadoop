package org.apache.hadoop.hdfs.remoteProxies;

public interface BatchedDirectoryListingInterface {
    java.lang.String toString();
    boolean hasMore();
    HdfsPartialListingInterface[] getListings();
    byte[] getStartAfter();
}