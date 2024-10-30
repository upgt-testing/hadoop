package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.metrics2.MetricsInfo;

import java.util.*;
import java.io.*;

public interface MetricsVisitorInterface {

    void gauge(MetricsInfo info, int value);

    void gauge(MetricsInfo info, long value);

    void gauge(MetricsInfo info, float value);

    void gauge(MetricsInfo info, double value);

    void counter(MetricsInfo info, int value);

    void counter(MetricsInfo info, long value);
}
