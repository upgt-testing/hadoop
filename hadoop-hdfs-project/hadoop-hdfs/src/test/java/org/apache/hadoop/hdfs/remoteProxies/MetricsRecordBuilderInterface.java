package org.apache.hadoop.hdfs.remoteProxies;

public interface MetricsRecordBuilderInterface {
    MetricsRecordBuilderInterface addCounter(org.apache.hadoop.metrics2.MetricsInfo arg0, long arg1);
    MetricsRecordBuilderInterface addGauge(org.apache.hadoop.metrics2.MetricsInfo arg0, long arg1);
    MetricsRecordBuilderInterface addGauge(org.apache.hadoop.metrics2.MetricsInfo arg0, double arg1);
    MetricsRecordBuilderInterface addCounter(org.apache.hadoop.metrics2.MetricsInfo arg0, int arg1);
    org.apache.hadoop.metrics2.MetricsCollector parent();
    MetricsRecordBuilderInterface addGauge(org.apache.hadoop.metrics2.MetricsInfo arg0, int arg1);
    MetricsRecordBuilderInterface add(AbstractMetricInterface arg0);
    MetricsRecordBuilderInterface addGauge(org.apache.hadoop.metrics2.MetricsInfo arg0, float arg1);
    MetricsRecordBuilderInterface tag(org.apache.hadoop.metrics2.MetricsInfo arg0, java.lang.String arg1);
    org.apache.hadoop.metrics2.MetricsCollector endRecord();
    MetricsRecordBuilderInterface setContext(java.lang.String arg0);
    MetricsRecordBuilderInterface add(MetricsTagInterface arg0);
}