package org.apache.hadoop.hdfs.remoteProxies;

public interface MutableCounterLongInterface {
    void snapshot(MetricsRecordBuilderInterface arg0);
    org.apache.hadoop.metrics2.MetricsInfo info();
    void incr();
    void clearChanged();
    long value();
    boolean changed();
    void incr(long arg0);
    void snapshot(MetricsRecordBuilderInterface arg0, boolean arg1);
    void setChanged();
}