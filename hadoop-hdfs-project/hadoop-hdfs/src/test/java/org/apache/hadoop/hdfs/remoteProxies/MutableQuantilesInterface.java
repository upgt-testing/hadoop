package org.apache.hadoop.hdfs.remoteProxies;

public interface MutableQuantilesInterface {
    void add(long arg0);
    org.apache.hadoop.metrics2.util.QuantileEstimator getEstimator();
    void snapshot(MetricsRecordBuilderInterface arg0);
    void setChanged();
    void stop();
    boolean changed();
    int getInterval();
    void snapshot(MetricsRecordBuilderInterface arg0, boolean arg1);
    void clearChanged();
    void setEstimator(org.apache.hadoop.metrics2.util.QuantileEstimator arg0);
}