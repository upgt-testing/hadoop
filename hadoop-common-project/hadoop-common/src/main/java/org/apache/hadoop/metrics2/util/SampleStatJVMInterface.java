package org.apache.hadoop.metrics2.util;

public interface SampleStatJVMInterface {

    double mean();

    double min();

    org.apache.hadoop.metrics2.util.SampleStatJVMInterface add(double arg0);

    java.lang.String toString();

    double total();

    double variance();

    void copyTo_bridge(org.apache.hadoop.metrics2.util.SampleStatJVMInterface arg0);

    long numSamples();

    org.apache.hadoop.metrics2.util.SampleStatJVMInterface add(long arg0, double arg1);

    double max();

    void reset();

    double stddev();
}
