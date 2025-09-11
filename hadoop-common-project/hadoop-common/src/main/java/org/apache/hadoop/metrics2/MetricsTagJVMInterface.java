package org.apache.hadoop.metrics2;

public interface MetricsTagJVMInterface extends MetricsInfoJVMInterface {

    int hashCode();

    java.lang.String value();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.lang.String name();

    java.lang.String description();

    java.lang.Object info();
}
