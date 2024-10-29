package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.namenode.CachePool;
import org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker;

public interface CachePoolInterface {

    String getPoolName();

    String getOwnerName();

    CachePoolInterface setOwnerName(String ownerName);

    String getGroupName();

    CachePoolInterface setGroupName(String groupName);

    FsPermission getMode();

    CachePoolInterface setMode(FsPermission mode);

    long getLimit();

    CachePoolInterface setLimit(long bytes);

    short getDefaultReplication();

    void setDefaultReplication(short replication);

    long getMaxRelativeExpiryMs();

    CachePoolInterface setMaxRelativeExpiryMs(long expiry);

    void resetStatistics();

    void addBytesNeeded(long bytes);

    void addBytesCached(long bytes);

    void addFilesNeeded(long files);

    void addFilesCached(long files);

    long getBytesNeeded();

    long getBytesCached();

    long getBytesOverlimit();

    long getFilesNeeded();

    long getFilesCached();

    CachePoolEntryInterface getEntry(FSPermissionChecker pc);

    String toString();

    CachePool.DirectiveList getDirectiveList();
}
