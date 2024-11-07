package org.apache.hadoop.hdfs.remoteProxies;

public interface ReclaimContextInterface {
    BlockStoragePolicySuiteInterface storagePolicySuite();
    ReclaimContextInterface getCopy();
    QuotaDeltaInterface quotaDelta();
    BlocksMapUpdateInfoInterface collectedBlocks();
}