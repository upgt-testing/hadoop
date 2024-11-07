package org.apache.hadoop.hdfs.remoteProxies;

public interface FileRegionInterface {
    int hashCode();
    boolean equals(java.lang.Object arg0);
    BlockInterface getBlock();
    ProvidedStorageLocationInterface getProvidedStorageLocation();
}