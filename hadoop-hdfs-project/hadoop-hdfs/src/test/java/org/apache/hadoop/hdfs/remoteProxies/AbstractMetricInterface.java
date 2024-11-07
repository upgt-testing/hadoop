package org.apache.hadoop.hdfs.remoteProxies;

public interface AbstractMetricInterface {
    void visit(org.apache.hadoop.metrics2.MetricsVisitor arg0);
    org.apache.hadoop.metrics2.MetricsInfo info();
    java.lang.String description();
    java.lang.String name();
    int hashCode();
    org.apache.hadoop.metrics2.MetricType type();
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    java.lang.Number value();
}