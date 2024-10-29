package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.fs.BatchedRemoteIterator;
import org.apache.hadoop.fs.CacheFlag;
import org.apache.hadoop.hdfs.protocol.*;
import org.apache.hadoop.hdfs.server.namenode.CacheManager;
import org.apache.hadoop.hdfs.server.namenode.CachePool;
import org.apache.hadoop.hdfs.server.namenode.FSPermissionChecker;

public interface CacheManagerInterface {

    boolean isEnabled();

    void startMonitorThread();

    void stopMonitorThread();

    void clearDirectiveStats();

    Collection<CachePool> getCachePools();

    Collection<CacheDirective> getCacheDirectives();

    GSetInterface getCachedBlocks();

    CacheDirectiveInfoInterface addDirective(CacheDirectiveInfo info, FSPermissionChecker pc, EnumSet<CacheFlag> flags);

    void modifyDirective(CacheDirectiveInfo info, FSPermissionChecker pc, EnumSet<CacheFlag> flags);

    void removeDirective(long id, FSPermissionChecker pc);

    BatchedRemoteIterator.BatchedListEntries<CacheDirectiveEntry> listCacheDirectives(long prevId, CacheDirectiveInfo filter, FSPermissionChecker pc);

    CachePoolInfo addCachePool(CachePoolInfo info);

    void modifyCachePool(CachePoolInfo info);

    void removeCachePool(String poolName);

    BatchedRemoteIterator.BatchedListEntries<CachePoolEntry> listCachePools(FSPermissionChecker pc, String prevKey);

    void setCachedLocations(LocatedBlocks locations);

    void processCacheReport(DatanodeID datanodeID, List<Long> blockIds);

    void saveStateCompat(DataOutputStream out, String sdPath);

    CacheManager.PersistState saveState();

    void loadStateCompat(DataInput in);

    void loadState(CacheManager.PersistState s);

    void waitForRescanIfNeeded();

    Thread getCacheReplicationMonitor();
}
