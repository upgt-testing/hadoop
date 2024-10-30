package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface MetricsRecordBuilderInterface {

    /**
    MetricsRecordBuilder tag(MetricsInfoInterface info, String value);

    MetricsRecordBuilder add(MetricsTagInterface tag);

    MetricsRecordBuilder add(AbstractMetricInterface metric);

    MetricsRecordBuilder setContext(String value);

    MetricsRecordBuilder addCounter(MetricsInfo info, int value);

    MetricsRecordBuilder addCounter(MetricsInfo info, long value);

    MetricsRecordBuilder addGauge(MetricsInfo info, int value);

    MetricsRecordBuilder addGauge(MetricsInfo info, long value);

    MetricsRecordBuilder addGauge(MetricsInfo info, float value);

    MetricsRecordBuilder addGauge(MetricsInfo info, double value);

    MetricsCollector parent();

    MetricsCollector endRecord();
     **/
}
