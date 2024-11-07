package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockLocationInterface {
    void setCorrupt(boolean arg0);
    boolean isCorrupt();
    void setCachedHosts(java.lang.String[] arg0);
    long getLength();
    void setLength(long arg0);
    boolean isStriped();
    java.lang.String[] getStorageIds();
    java.lang.String toString();
    java.lang.String[] getNames() throws java.io.IOException;
    void setOffset(long arg0);
    long getOffset();
    void setStorageIds(java.lang.String[] arg0);
    void setStorageTypes(org.apache.hadoop.fs.StorageType[] arg0);
    java.lang.String[] getCachedHosts();
    java.lang.String[] getTopologyPaths() throws java.io.IOException;
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    void setNames(java.lang.String[] arg0) throws java.io.IOException;
    void setTopologyPaths(java.lang.String[] arg0) throws java.io.IOException;
    java.lang.String[] getHosts() throws java.io.IOException;
    void setHosts(java.lang.String[] arg0) throws java.io.IOException;
}