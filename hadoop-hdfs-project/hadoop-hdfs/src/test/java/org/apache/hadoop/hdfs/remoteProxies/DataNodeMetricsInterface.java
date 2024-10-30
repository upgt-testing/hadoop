package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface DataNodeMetricsInterface {

    String name();

    JvmMetricsInterface getJvmMetrics();

    void addHeartbeat(long latency, String rpcMetricSuffix);

    void addHeartbeatTotal(long latency, String rpcMetricSuffix);

    void addLifeline(long latency, String rpcMetricSuffix);

    void addBlockReport(long latency, String rpcMetricSuffix);

    void addIncrementalBlockReport(long latency, String rpcMetricSuffix);

    void addCacheReport(long latency);

    void incrBlocksReplicated();

    void incrBlocksWritten();

    void incrBlocksRemoved(int delta);

    void incrBytesWritten(int delta);

    void incrBlockVerificationFailures();

    void incrBlocksVerified();

    void incrBlocksCached(int delta);

    void incrBlocksUncached(int delta);

    void addReadBlockOp(long latency);

    void addWriteBlockOp(long latency);

    void addReplaceBlockOp(long latency);

    void addCopyBlockOp(long latency);

    void addBlockChecksumOp(long latency);

    void incrBytesRead(int delta);

    void incrBlocksRead();

    void incrFsyncCount();

    void incrTotalWriteTime(long timeTaken);

    void incrTotalReadTime(long timeTaken);

    void addPacketAckRoundTripTimeNanos(long latencyNanos);

    void addFlushNanos(long latencyNanos);

    void addFsyncNanos(long latencyNanos);

    void shutdown();

    void incrWritesFromClient(boolean local, long size);

    void incrReadsFromClient(boolean local, long size);

    void incrVolumeFailures(int size);

    void incrDatanodeNetworkErrors();

    void incrBlocksGetLocalPathInfo();

    void addSendDataPacketBlockedOnNetworkNanos(long latencyNanos);

    void addSendDataPacketTransferNanos(long latencyNanos);

    void incrRamDiskBlocksWrite();

    void incrRamDiskBlocksWriteFallback();

    void addRamDiskBytesWrite(long bytes);

    void incrRamDiskBlocksReadHits();

    void incrRamDiskBlocksEvicted();

    void incrRamDiskBlocksEvictedWithoutRead();

    void addRamDiskBlocksEvictionWindowMs(long latencyMs);

    void incrRamDiskBlocksLazyPersisted();

    void incrRamDiskBlocksDeletedBeforeLazyPersisted();

    void incrRamDiskBytesLazyPersisted(long bytes);

    void addRamDiskBlocksLazyPersistWindowMs(long latencyMs);

    void resetBlocksInPendingIBR();

    void incrBlocksInPendingIBR();

    void incrBlocksReceivingInPendingIBR();

    void incrBlocksReceivedInPendingIBR();

    void incrBlocksDeletedInPendingIBR();

    void incrECReconstructionTasks();

    void incrECFailedReconstructionTasks();

    void incrECInvalidReconstructionTasks();

    long getECInvalidReconstructionTasks();

    void incrDataNodeActiveXceiversCount();

    void decrDataNodeActiveXceiversCount();

    void setDataNodeActiveXceiversCount(int value);

    int getDataNodeActiveXceiverCount();

    void incrDataNodePacketResponderCount();

    void decrDataNodePacketResponderCount();

    void setDataNodePacketResponderCount(int value);

    int getDataNodePacketResponderCount();

    void incrDataNodeBlockRecoveryWorkerCount();

    void decrDataNodeBlockRecoveryWorkerCount();

    void setDataNodeBlockRecoveryWorkerCount(int value);

    int getDataNodeBlockRecoveryWorkerCount();

    void incrECDecodingTime(long decodingTimeNanos);

    void incrECReconstructionBytesRead(long bytes);

    void incrECReconstructionRemoteBytesRead(long bytes);

    void incrECReconstructionBytesWritten(long bytes);

    void incrECReconstructionReadTime(long millis);

    void incrECReconstructionWriteTime(long millis);

    void incrECReconstructionDecodingTime(long millis);

    void incrECReconstructionValidateTime(long millis);

    DataNodeUsageReportInterface getDNUsageReport(long timeSinceLastReport);

    void incrActorCmdQueueLength(int delta);

    void incrNumProcessedCommands();

    void addNumProcessedCommands(long latency);
}
