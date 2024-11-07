package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockScannerInterface {
    void removeAllVolumeScanners();
    boolean hasAnyRegisteredScanner();
    void disableBlockPoolId(java.lang.String arg0);
    void setJoinVolumeScannersTimeOutMs(long arg0);
    boolean isEnabled();
    long getJoinVolumeScannersTimeOutMs();
    StatisticsInterface getVolumeStats(java.lang.String arg0);
    void printStats(java.lang.StringBuilder arg0);
    void enableBlockPoolId(java.lang.String arg0);
    void setConf(ConfInterface arg0);
    void addVolumeScanner(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeReference arg0);
    void removeVolumeScanner(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0);
    void markSuspectBlock(java.lang.String arg0, ExtendedBlockInterface arg1);
}