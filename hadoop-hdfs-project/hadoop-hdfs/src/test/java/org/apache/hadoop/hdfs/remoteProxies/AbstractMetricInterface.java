package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.metrics2.MetricType;

import java.util.*;
import java.io.*;

public interface AbstractMetricInterface {

    String name();

    String description();

    Number value();

    MetricType type();

    void visit(MetricsVisitorInterface visitor);

    boolean equals(Object obj);

    int hashCode();

    String toString();
}
