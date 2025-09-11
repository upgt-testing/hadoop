package org.apache.hadoop.metrics2.lib;

public interface MutableMetricJVMInterface {

    void snapshot_bridge(org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface arg0, boolean arg1);

    void snapshot_bridge(org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface arg0);

    boolean changed();
}
