package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.conf.Configuration;

import java.util.*;
import java.io.*;

public interface DNConfInterface {

    Configuration getConf();

    boolean getEncryptDataTransfer();

    String getEncryptionAlgorithm();

    long getXceiverStopTimeout();

    long getMaxLockedMemory();

    boolean getConnectToDnViaHostname();

    int getSocketTimeout();

    int getSocketWriteTimeout();

    int getEcChecksumSocketTimeout();

    SaslPropertiesResolverInterface getSaslPropsResolver();

    TrustedChannelResolverInterface getTrustedChannelResolver();

    boolean getIgnoreSecurePortsForTesting();

    boolean getAllowNonLocalLazyPersist();

    int getTransferSocketRecvBufferSize();

    int getTransferSocketSendBufferSize();

    boolean getDataTransferServerTcpNoDelay();

    long getBpReadyTimeout();

    long getLifelineIntervalMs();

    int getVolFailuresTolerated();

    int getVolsConfigured();

    long getSlowIoWarningThresholdMs();

    String[] getPmemVolumes();

    boolean getPmemCacheRecoveryEnabled();

    long getProcessCommandsThresholdMs();

    long getBlockReportInterval();

    long getCacheReportInterval();

    void setFileIoProfilingSamplingPercentage(int samplingPercentage);

    void setOutliersReportIntervalMs(String reportIntervalMs);
}
