package org.apache.hadoop.hdfs.remoteProxies;

public interface MinMaxInterface {
    double max();
    void add(double arg0);
    double min();
    void reset(MinMaxInterface arg0);
    void reset();
}