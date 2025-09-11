package org.apache.hadoop.metrics2;

public interface MetricsRecordBuilderJVMInterface {

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, int arg1);

    java.lang.Object endRecord();

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, double arg1);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface add_bridge(org.apache.hadoop.metrics2.AbstractMetricJVMInterface arg0);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface tag_bridge(java.lang.Object arg0, java.lang.String arg1);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, long arg1);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface add_bridge(org.apache.hadoop.metrics2.MetricsTagJVMInterface arg0);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addCounter_bridge(java.lang.Object arg0, long arg1);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addCounter_bridge(java.lang.Object arg0, int arg1);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface addGauge_bridge(java.lang.Object arg0, float arg1);

    org.apache.hadoop.metrics2.MetricsRecordBuilderJVMInterface setContext(java.lang.String arg0);

    java.lang.Object parent();
}
