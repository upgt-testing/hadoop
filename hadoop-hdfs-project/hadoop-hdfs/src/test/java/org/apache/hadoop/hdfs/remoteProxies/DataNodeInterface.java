package org.apache.hadoop.hdfs.remoteProxies;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.DFSUtilClient;
import org.apache.hadoop.hdfs.protocol.DatanodeVolumeInfo;
import org.apache.hadoop.hdfs.protocol.ExtendedBlock;
import org.apache.hadoop.hdfs.protocol.datatransfer.PipelineAck;
import org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier;
import org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi;
//import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.protocol.datatransfer.sasl.DataEncryptionKeyFactory;
import org.apache.hadoop.hdfs.protocol.DatanodeID;
import org.apache.hadoop.hdfs.server.protocol.BlockRecoveryCommand;
import org.apache.hadoop.hdfs.util.DataTransferThrottler;
import org.apache.hadoop.hdfs.server.namenode.startupprogress.Status;
import org.apache.hadoop.security.token.Token;

public interface DataNodeInterface {

    String reconfigurePropertyImpl(String property, String newVal);

    Collection<String> getReconfigurableProperties();

    PipelineAck.ECN getECN();

    FileIoProviderInterface getFileIoProvider();

    void notifyNamenodeReceivedBlock(ExtendedBlockInterface block, String delHint, String storageUuid, boolean isOnTransientStorage);

    void notifyNamenodeDeletedBlock(ExtendedBlock block, String storageUuid);

    void reportBadBlocks(ExtendedBlock block);

    void reportBadBlocks(ExtendedBlock block, FsVolumeSpi volume);

    void reportRemoteBadBlock(DatanodeInfoInterface srcDataNode, ExtendedBlock block);

    void reportCorruptedBlocks(DFSUtilClient.CorruptedBlocks corruptedBlocks);

    void setHeartbeatsDisabledForTests(boolean heartbeatsDisabledForTests);

    SaslDataTransferClientInterface getSaslClient();

    int getBpOsCount();

    boolean testRMIPrint(String message);

    DataXceiverServerInterface getXferServer();

    int getXferPort();

    SaslDataTransferServerInterface getSaslServer();

    String getDisplayName();

    InetSocketAddress getXferAddress();

    int getIpcPort();

    DatanodeRegistrationInterface getDNRegistrationForBP(String bpid);

    Socket newSocket();

    DataNodeMetricsInterface getMetrics();

    DataNodeDiskMetricsInterface getDiskMetrics();

    DataNodePeerMetricsInterface getPeerMetrics();

    long getMaxNumberOfBlocksToLog();

    //BlockLocalPathInfoInterface getBlockLocalPathInfo(ExtendedBlock block, Token<BlockTokenIdentifier> token);

    void shutdown();

    void checkDiskErrorAsync(FsVolumeSpi volume);

    int getXceiverCount();

    int getActiveTransferThreadCount();

    Map<String, Map<String, Long>> getDatanodeNetworkCounts();

    int getXmitsInProgress();

    void incrementXmitsInProgress();

    void incrementXmitsInProcess(int delta);

    void decrementXmitsInProgress();

    void decrementXmitsInProgress(int delta);

    Token<BlockTokenIdentifier> getBlockAccessToken(ExtendedBlock b, EnumSet<BlockTokenIdentifier.AccessMode> mode, StorageType[] storageTypes, String[] storageIds);

    DataEncryptionKeyFactory getDataEncryptionKeyFactoryForBlock(ExtendedBlock block);

    void runDatanodeDaemon();

    boolean isDatanodeUp();

    String toString();

    void scheduleAllBlockReport(long delay);

    FsDatasetSpiInterface getFSDataset();

    BlockScannerInterface getBlockScanner();

    BlockPoolTokenSecretManagerInterface getBlockPoolTokenSecretManager();

    ReplicaRecoveryInfoInterface initReplicaRecovery(BlockRecoveryCommand.RecoveringBlock rBlock);

    String updateReplicaUnderRecovery(ExtendedBlock oldBlock, long recoveryId, long newBlockId, long newLength);

    long getReplicaVisibleLength(ExtendedBlock block);

    String getSoftwareVersion();

    String getVersion();

    String getRpcPort();

    String getDataPort();

    String getHttpPort();

    long getDNStartedTimeInMillis();

    String getRevision();

    int getInfoPort();

    int getInfoSecurePort();

    String getNamenodeAddresses();

    String getDatanodeHostname();

    String getBPServiceActorInfo();

    String getVolumeInfo();

    String getClusterId();

    String getDiskBalancerStatus();

    boolean isSecurityEnabled();

    void refreshNamenodes(Configuration conf);

    void refreshNamenodes();

    void deleteBlockPool(String blockPoolId, boolean force);

    void shutdownDatanode(boolean forUpgrade);

    void evictWriters();

    DatanodeLocalInfoInterface getDatanodeInfo();

    void startReconfiguration();

    ReconfigurationTaskStatusInterface getReconfigurationStatus();

    List<String> listReconfigurableProperties();

    void triggerBlockReport(BlockReportOptionsInterface options);

    boolean isConnectedToNN(InetSocketAddress addr);

    boolean isBPServiceAlive(String bpid);

    boolean isDatanodeFullyStarted();

    DatanodeID getDatanodeId();

    void clearAllBlockSecretKeys();

    long getBalancerBandwidth();

    DNConfInterface getDnConf();

    String getDatanodeUuid();

    ShortCircuitRegistryInterface getShortCircuitRegistry();

    DataTransferThrottler getEcReconstuctReadThrottler();

    DataTransferThrottler getEcReconstuctWriteThrottler();

    void checkDiskError();

    long getLastDiskErrorCheck();

    BlockRecoveryWorkerInterface getBlockRecoveryWorker();

    ErasureCodingWorkerInterface getErasureCodingWorker();

    long getOOBTimeout(Status status);

    TracerInterface getTracer();

    void submitDiskBalancerPlan(String planID, long planVersion, String planFile, String planData, boolean skipDateCheck);

    void cancelDiskBalancePlan(String planID);

    DiskBalancerWorkStatusInterface queryDiskBalancerPlan();

    String getDiskBalancerSetting(String key);

    String getSendPacketDownstreamAvgInfo();

    String getSlowDisks();

    List<DatanodeVolumeInfo> getVolumeReport();

    BlockPoolManagerInterface getBlockPoolManager();
}
