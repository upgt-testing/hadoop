package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.metrics2.MetricsRecordBuilder;

public interface MutableRollingAveragesInterface {

    void snapshot(MetricsRecordBuilder builder, boolean all);

    void collectThreadLocalStates();

    void add(String name, long value);

    void close();

    Map<String, Double> getStats(long minSamples);

    void setRecordValidityMs(long value);
}
