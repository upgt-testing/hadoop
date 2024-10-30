package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface JvmMetricsInterface {

    void registerIfNeeded();

    void setPauseMonitor(JvmPauseMonitorInterface pauseMonitor);

    void setGcTimeMonitor(GcTimeMonitorInterface gcTimeMonitor);

    void getMetrics(MetricsCollectorInterface collector, boolean all);
}
