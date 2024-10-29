package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.permission.FsPermission;

public interface CachePoolInfoInterface {

    String getPoolName();

    String getOwnerName();

    CachePoolInfoInterface setOwnerName(String ownerName);

    String getGroupName();

    CachePoolInfoInterface setGroupName(String groupName);

    FsPermission getMode();

    CachePoolInfoInterface setMode(FsPermission mode);

    Long getLimit();

    CachePoolInfoInterface setLimit(Long bytes);

    Short getDefaultReplication();

    CachePoolInfoInterface setDefaultReplication(Short repl);

    Long getMaxRelativeExpiryMs();

    CachePoolInfoInterface setMaxRelativeExpiryMs(Long ms);

    String toString();

    boolean equals(Object o);

    int hashCode();
}
