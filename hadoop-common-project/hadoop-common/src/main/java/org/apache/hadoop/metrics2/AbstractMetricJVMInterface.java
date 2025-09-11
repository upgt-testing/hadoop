package org.apache.hadoop.metrics2;

public interface AbstractMetricJVMInterface extends MetricsInfoJVMInterface {

    int hashCode();

    java.lang.Object type();

    java.lang.Number value();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.lang.String name();

    java.lang.String description();

    void visit_bridge(java.lang.Object arg0);
}
