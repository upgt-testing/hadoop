package org.apache.hadoop.hdfs.remoteProxies;

public interface MetricsTagInterface {
    boolean equals(java.lang.Object arg0);
    java.lang.String description();
    org.apache.hadoop.metrics2.MetricsInfo info();
    java.lang.String name();
    java.lang.String toString();
    int hashCode();
    java.lang.String value();
}