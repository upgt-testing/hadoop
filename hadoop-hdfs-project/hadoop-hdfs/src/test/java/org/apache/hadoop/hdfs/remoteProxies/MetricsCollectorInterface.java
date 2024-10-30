package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.metrics2.MetricsInfo;

import java.util.*;
import java.io.*;

public interface MetricsCollectorInterface {

    MetricsRecordBuilderInterface addRecord(String name);

    MetricsRecordBuilderInterface addRecord(MetricsInfo info);
}
