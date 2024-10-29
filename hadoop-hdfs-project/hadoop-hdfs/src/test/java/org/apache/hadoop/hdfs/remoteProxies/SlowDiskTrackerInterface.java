package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.protocol.SlowDiskReports;

public interface SlowDiskTrackerInterface {

    void addSlowDiskReport(String dataNodeID, SlowDiskReports dnSlowDiskReport);

    void checkAndUpdateReportIfNecessary();

    void updateSlowDiskReportAsync(long now);

    String getSlowDiskReportAsJsonString();
}
