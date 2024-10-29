package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.StorageType;

import java.util.*;
import java.io.*;

public interface BlockLocationInterface {

    String[] getHosts();

    String[] getCachedHosts();

    String[] getNames();

    String[] getTopologyPaths();

    String[] getStorageIds();

    StorageType[] getStorageTypes();

    long getOffset();

    long getLength();

    boolean isCorrupt();

    boolean isStriped();

    void setOffset(long offset);

    void setLength(long length);

    void setCorrupt(boolean corrupt);

    void setHosts(String[] hosts);

    void setCachedHosts(String[] cachedHosts);

    void setNames(String[] names);

    void setTopologyPaths(String[] topologyPaths);

    void setStorageIds(String[] storageIds);

    void setStorageTypes(StorageType[] storageTypes);

    String toString();
}
