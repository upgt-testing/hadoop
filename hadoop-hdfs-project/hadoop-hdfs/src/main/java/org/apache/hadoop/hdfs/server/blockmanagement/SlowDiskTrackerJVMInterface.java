package org.apache.hadoop.hdfs.server.blockmanagement;

import java.util.ArrayList;

public interface SlowDiskTrackerJVMInterface {
    String getSlowDiskReportAsJsonString();
    void setReportValidityMs(long validityMs);
    ArrayList<? extends DiskLatencyJVMInterface> getSlowDisksReport();
}
