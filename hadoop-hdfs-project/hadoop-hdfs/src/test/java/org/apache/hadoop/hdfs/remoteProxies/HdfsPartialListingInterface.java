package org.apache.hadoop.hdfs.remoteProxies;

public interface HdfsPartialListingInterface {
    RemoteExceptionInterface getException();
    java.util.List<org.apache.hadoop.hdfs.protocol.HdfsFileStatus> getPartialListing();
    java.lang.String toString();
    int getParentIdx();
}