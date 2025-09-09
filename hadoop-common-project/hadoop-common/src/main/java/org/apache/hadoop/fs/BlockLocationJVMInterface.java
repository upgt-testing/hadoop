package org.apache.hadoop.fs;

public interface BlockLocationJVMInterface {

    long getLength();

    void setOffset(long arg0);

    boolean isStriped();

    java.lang.String toString();

    void setStorageIds(java.lang.String[] arg0);

    void setCorrupt(boolean arg0);

    java.lang.String[] getNames() throws java.io.IOException;

    java.lang.Object[] getStorageTypes();

    void setTopologyPaths(java.lang.String[] arg0) throws java.io.IOException;

    void setCachedHosts(java.lang.String[] arg0);

    java.lang.String[] getHosts() throws java.io.IOException;

    java.lang.String[] getStorageIds();

    void setNames(java.lang.String[] arg0) throws java.io.IOException;

    java.lang.String[] getTopologyPaths() throws java.io.IOException;

    java.lang.String[] getCachedHosts();

    boolean isCorrupt();

    long getOffset();

    void setLength(long arg0);

    void setHosts(java.lang.String[] arg0) throws java.io.IOException;
}
