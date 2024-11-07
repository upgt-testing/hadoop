package org.apache.hadoop.hdfs.remoteProxies;

public interface ProfilingFileIoEventsInterface {
    void setSampleRangeMax(int arg0);
    boolean getDiskStatsEnabled();
    DataNodeVolumeMetricsInterface getVolumeMetrics(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0);
    void onFailure(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, long arg1);
    long beforeMetadataOp(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, org.apache.hadoop.hdfs.server.datanode.FileIoProvider.OPERATION arg1);
    void afterMetadataOp(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, org.apache.hadoop.hdfs.server.datanode.FileIoProvider.OPERATION arg1, long arg2);
    int getSampleRangeMax();
    void afterFileIo(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, org.apache.hadoop.hdfs.server.datanode.FileIoProvider.OPERATION arg1, long arg2, long arg3);
    long beforeFileIo(org.apache.hadoop.hdfs.server.datanode.fsdataset.FsVolumeSpi arg0, org.apache.hadoop.hdfs.server.datanode.FileIoProvider.OPERATION arg1, long arg2);
}