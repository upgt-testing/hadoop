package org.apache.hadoop.hdfs.remoteProxies;

public interface MutableRollingAveragesInterface {
    void snapshot(MetricsRecordBuilderInterface arg0);
    void setChanged();
    void add(java.lang.String arg0, long arg1);
    void clearChanged();
    void setRecordValidityMs(long arg0);
    void replaceScheduledTask(int arg0, long arg1, java.util.concurrent.TimeUnit arg2);
    void close() throws java.io.IOException;
    void snapshot(MetricsRecordBuilderInterface arg0, boolean arg1);
    void collectThreadLocalStates();
    boolean changed();
    java.util.Map<java.lang.String, java.lang.Double> getStats(long arg0);
    void rollOverAvgs();
}