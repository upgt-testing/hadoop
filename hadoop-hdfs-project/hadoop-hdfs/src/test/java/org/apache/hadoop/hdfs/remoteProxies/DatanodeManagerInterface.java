package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;
import org.apache.hadoop.hdfs.server.protocol.StorageReport;
import org.apache.hadoop.hdfs.server.protocol.VolumeFailureSummary;

public interface DatanodeManagerInterface {

    void initSlowPeerTracker(Configuration conf, TimerInterface timer, boolean dataNodePeerStatsEnabled);

    void stopSlowPeerCollector();

    NetworkTopologyInterface getNetworkTopology();

    DatanodeAdminManagerInterface getDatanodeAdminManager();

    HostConfigManagerInterface getHostConfigManager();

    void setHeartbeatExpireInterval(long expiryMs);

    FSClusterStatsInterface getFSClusterStats();

    int getBlockInvalidateLimit();

    DatanodeStatisticsInterface getDatanodeStatistics();

    void setAvoidSlowDataNodesForReadEnabled(boolean enable);

    boolean getEnableAvoidSlowDataNodesForRead();

    void setMaxSlowpeerCollectNodes(int maxNodes);

    int getMaxSlowpeerCollectNodes();

    void sortLocatedBlocks(String targetHost, List<LocatedBlock> locatedBlocks);

    DatanodeDescriptorInterface getDatanodeByHost(String host);

    DatanodeDescriptorInterface getDatanodeByXferAddr(String host, int xferPort);

    Set<DatanodeDescriptorInterface> getDatanodes();

    Host2NodesMapInterface getHost2DatanodeMap();

    DatanodeDescriptorInterface getDatanode(String datanodeUuid);

    DatanodeDescriptorInterface getDatanode(DatanodeID nodeID);

    DatanodeStorageInfoInterface[] getDatanodeStorageInfos(DatanodeID[] datanodeID, String[] storageIDs, String format, Object args);

    void removeDatanode(DatanodeID node);

    HashMap<String, Integer> getDatanodesSoftwareVersions();

    List<String> resolveNetworkLocation(List<String> names);

    void registerDatanode(DatanodeRegistrationInterface nodeReg);

    void refreshNodes(Configuration conf);

    int getNumLiveDataNodes();

    int getNumDeadDataNodes();

    int getNumOfDataNodes();

    List<DatanodeDescriptorInterface> getDecommissioningNodes();

    List<DatanodeDescriptorInterface> getEnteringMaintenanceNodes();

    boolean shouldAvoidStaleDataNodesForWrite();

    long getBlocksPerPostponedMisreplicatedBlocksRescan();

    long getHeartbeatInterval();

    long getHeartbeatRecheckInterval();

    int getNumStaleNodes();

    int getNumStaleStorages();

    void fetchDatanodes(List<DatanodeDescriptor> live, List<DatanodeDescriptor> dead, boolean removeDecommissionNode);

    List<DatanodeDescriptorInterface> getDatanodeListForReport(HdfsConstants.DatanodeReportType type);

    List<DatanodeDescriptorInterface> getAllSlowDataNodes();

    DatanodeCommandInterface[] handleHeartbeat(DatanodeRegistration nodeReg, StorageReport[] reports, String blockPoolId, long cacheCapacity, long cacheUsed, int xceiverCount, int maxTransfers, int failedVolumes, VolumeFailureSummary volumeFailureSummary, SlowPeerReportsInterface slowPeers, SlowDiskReportsInterface slowDisks);

    void handleLifeline(DatanodeRegistration nodeReg, StorageReport[] reports, long cacheCapacity, long cacheUsed, int xceiverCount, int failedVolumes, VolumeFailureSummary volumeFailureSummary);

    void setBalancerBandwidth(long bandwidth);

    void markAllDatanodesStale();

    void clearPendingQueues();

    void resetLastCachingDirectiveSentTime();

    String toString();

    void clearPendingCachingCommands();

    void setShouldSendCachingCommands(boolean shouldSendCachingCommands);

    void setHeartbeatInterval(long intervalSeconds);

    void setHeartbeatRecheckInterval(int recheckInterval);

    void setBlockInvalidateLimit(int configuredBlockInvalidateLimit);

    String getSlowPeersReport();

    Set<String> getSlowPeersUuidSet();

    SlowPeerTrackerInterface getSlowPeerTracker();

    SlowDiskTrackerInterface getSlowDiskTracker();

    void addSlowPeers(String dnUuid);

    String getSlowDisksReport();

    DatanodeStorageReportInterface[] getDatanodeStorageReport(HdfsConstants.DatanodeReportType type);
}
