package org.apache.hadoop.hdfs.remoteProxies;

public interface SchedulerInterface {
    long scheduleBlockReport(long arg0, boolean arg1);
    boolean isBlockReportDue(long arg0);
    void updateLastHeartbeatTime(long arg0);
    long scheduleNextHeartbeat();
    boolean isOutliersReportDue(long arg0);
    long monotonicNow();
    void setOutliersReportIntervalMs(long arg0);
    void updateLastBlockReportTime(long arg0);
    void forceFullBlockReportNow();
    boolean isHeartbeatDue(long arg0);
    long getLastHearbeatTime();
    void scheduleNextBlockReport();
    void scheduleNextOutlierReport();
    long scheduleNextLifeline(long arg0);
    long getLifelineWaitTime();
    boolean isLifelineDue(long arg0);
    long getOutliersReportIntervalMs();
    long getNextBlockReportTime();
    long getLastBlockReportTime();
    void setBlockReportIntervalMs(long arg0);
    long getBlockReportIntervalMs();
    long scheduleHeartbeat();
    long getHeartbeatWaitTime();
    void setNextBlockReportTime(long arg0);
}