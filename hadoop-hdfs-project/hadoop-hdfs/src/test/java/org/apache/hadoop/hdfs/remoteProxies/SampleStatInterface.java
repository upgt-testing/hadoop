package org.apache.hadoop.hdfs.remoteProxies;

public interface SampleStatInterface {
    SampleStatInterface add(double arg0);
    void reset();
    void reset(long arg0, double arg1, double arg2, double arg3, double arg4, double arg5, MinMaxInterface arg6);
    double variance();
    double mean();
    double total();
    double max();
    double min();
    long numSamples();
    double stddev();
    void copyTo(SampleStatInterface arg0);
    SampleStatInterface add(long arg0, double arg1);
    java.lang.String toString();
}