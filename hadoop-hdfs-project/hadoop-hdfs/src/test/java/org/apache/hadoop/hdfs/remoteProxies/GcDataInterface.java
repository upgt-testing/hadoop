package org.apache.hadoop.hdfs.remoteProxies;

public interface GcDataInterface {
    long getAccumulatedGcTime();
    long getTimestamp();
    long getGcMonitorRunTime();
    int getGcTimePercentage();
    long getAccumulatedGcCount();
    void update(long arg0, long arg1, long arg2, long arg3, int arg4);
    GcDataInterface clone();
}