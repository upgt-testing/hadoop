package org.apache.hadoop.metrics2;

public interface MetricsTagJVMInterface extends MetricsInfoJVMInterface {

    int hashCode();

    boolean equals(java.lang.Object arg0);

    java.lang.String value();

    java.lang.String toString();

    java.lang.String description();

    java.lang.String name();

    java.lang.Object info();
}
