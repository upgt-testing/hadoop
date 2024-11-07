package org.apache.hadoop.hdfs.remoteProxies;

public interface MetricsSystemInterface {
    java.lang.String currentConfig();
    void unregisterSource(java.lang.String arg0);
    <T> T register(java.lang.String arg0, java.lang.String arg1, T arg2);
    void stopMetricsMBeans();
//    <T> T register(java.lang.String arg0, java.lang.String arg1, T arg2);
    void publishMetricsNow();
    MetricsSystemInterface init(java.lang.String arg0);
    void stop();
    void register(org.apache.hadoop.metrics2.MetricsSystem.Callback arg0);
    void startMetricsMBeans();
    void start();
    org.apache.hadoop.metrics2.MetricsSource getSource(java.lang.String arg0);
    boolean shutdown();
    <T> T register(T arg0);
}