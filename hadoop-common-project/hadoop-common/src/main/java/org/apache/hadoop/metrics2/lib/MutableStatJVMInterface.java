package org.apache.hadoop.metrics2.lib;

public interface MutableStatJVMInterface extends MutableMetricJVMInterface {

    void setUpdateTimeStamp(boolean arg0);

    java.lang.String toString();

    void add(long arg0, long arg1);

    void snapshot_bridge(org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface arg0, boolean arg1);

    long getSnapshotTimeStamp();

    void resetMinMax();

    void add(long arg0);

    org.apache.hadoop.metrics2.util.SampleStatJVMInterface lastStat();

    void setExtended(boolean arg0);
}
