package org.apache.hadoop.hdfs.remoteProxies;

public interface MutableRateInterface {
    void clearChanged();
    void add(long arg0, long arg1);
    void snapshot(MetricsRecordBuilderInterface arg0);
    void add(long arg0);
    long getSnapshotTimeStamp();
    void resetMinMax();
    SampleStatInterface lastStat();
    java.lang.String toString();
    void snapshot(MetricsRecordBuilderInterface arg0, boolean arg1);
    void setExtended(boolean arg0);
    void setChanged();
    void setUpdateTimeStamp(boolean arg0);
    boolean changed();
}