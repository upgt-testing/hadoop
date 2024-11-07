package org.apache.hadoop.hdfs.remoteProxies;

public interface JvmMetricsInterface {
    JvmMetricsInterface initSingleton(java.lang.String arg0, java.lang.String arg1);
    void shutdownSingleton();
    void setPauseMonitor(JvmPauseMonitorInterface arg0);
    void reattach(MetricsSystemInterface arg0, JvmMetricsInterface arg1);
    void setGcTimeMonitor(GcTimeMonitorInterface arg0);
    void getThreadUsageFromGroup(MetricsRecordBuilderInterface arg0);
    void getMemoryUsage(MetricsRecordBuilderInterface arg0);
    void getMetrics(org.apache.hadoop.metrics2.MetricsCollector arg0, boolean arg1);
    JvmMetricsInterface create(java.lang.String arg0, java.lang.String arg1, MetricsSystemInterface arg2);
    void getEventCounters(MetricsRecordBuilderInterface arg0);
    void getGcUsage(MetricsRecordBuilderInterface arg0);
    org.apache.hadoop.metrics2.MetricsInfo[] getGcInfo(java.lang.String arg0);
    void getThreadUsage(MetricsRecordBuilderInterface arg0);
    float calculateMaxMemoryUsage(java.lang.management.MemoryUsage arg0);
    void registerIfNeeded();
}