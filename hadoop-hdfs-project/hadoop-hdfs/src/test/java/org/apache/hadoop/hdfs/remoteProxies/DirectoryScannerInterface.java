package org.apache.hadoop.hdfs.remoteProxies;

public interface DirectoryScannerInterface {
    void setRetainDiffs(boolean arg0);
    void run();
    void addDifference(java.util.Collection<org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi.ScanInfo> arg0, StatsInterface arg1, long arg2, org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg3);
    void addDifference(java.util.Collection<org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi.ScanInfo> arg0, StatsInterface arg1, ScanInfoInterface arg2);
    void shutdown();
    java.util.Collection<org.apache.hadoop.hdfs.server.datanode.DirectoryScanner.ScanInfoVolumeReport> getVolumeReports();
    void reconcile() throws java.io.IOException;
    boolean getRunStatus();
    void clear();
    void start();
    void scan();
}