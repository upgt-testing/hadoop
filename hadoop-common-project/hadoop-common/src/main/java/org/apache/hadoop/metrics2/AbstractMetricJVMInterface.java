package org.apache.hadoop.metrics2;

public interface AbstractMetricJVMInterface extends MetricsInfoJVMInterface {

    int hashCode();

    java.lang.Object type();

    boolean equals(java.lang.Object arg0);

    java.lang.Number value();

    java.lang.String toString();

    void visit_bridge(java.lang.Object arg0);

    java.lang.String name();

    java.lang.String description();
}
