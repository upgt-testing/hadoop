package org.apache.hadoop.hdfs.remoteProxies;

public interface StorageLocationInterface {
    boolean matchesStorageDirectory(StorageDirectoryInterface arg0) throws java.io.IOException;
    java.net.URI getBpURI(java.lang.String arg0, java.lang.String arg1);
    int compareTo(StorageLocationInterface arg0);
    org.apache.hadoop.fs.StorageType getStorageType();
    boolean matchesStorageDirectory(StorageDirectoryInterface arg0, java.lang.String arg1) throws java.io.IOException;
    org.apache.hadoop.hdfs.server.datanode.checker.VolumeCheckResult check(CheckContextInterface arg0) throws java.io.IOException;
    java.net.URI getUri();
    java.net.URI getNormalizedUri();
    int hashCode();
    boolean equals(java.lang.Object arg0);
    java.net.URI normalizeFileURI(java.net.URI arg0);
//    V check(K arg0) throws java.lang.Exception;
    StorageLocationInterface parse(java.lang.String arg0) throws java.io.IOException, java.lang.SecurityException;
    void makeBlockPoolDir(java.lang.String arg0, ConfigurationInterface arg1) throws java.io.IOException;
    java.lang.String toString();
}