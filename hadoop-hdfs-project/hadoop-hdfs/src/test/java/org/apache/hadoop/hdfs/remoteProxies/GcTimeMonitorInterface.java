package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.util.GcTimeMonitor;

import java.util.*;
import java.io.*;

public interface GcTimeMonitorInterface {

    void run();

    void shutdown();

    GcTimeMonitor.GcData getLatestGcData();
}
