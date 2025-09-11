package org.apache.hadoop.metrics2.util;

public interface SampleStatJVMInterface {

    double mean();

    double min();

    java.lang.String toString();

    org.apache.hadoop.metrics2.util.SampleStatJVMInterface add(double arg0);

    double total();

    double variance();

    long numSamples();

    void copyTo_bridge(org.apache.hadoop.metrics2.util.SampleStatJVMInterface arg0);

    org.apache.hadoop.metrics2.util.SampleStatJVMInterface add(long arg0, double arg1);

    double max();

    void reset();

    double stddev();
}
