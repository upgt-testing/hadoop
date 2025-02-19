package org.apache.hadoop.hdfs.server.blockmanagement;

import java.util.ArrayList;

public interface SlowDiskTrackerJVMInterface {
    void setReportValidityMs(long validityMs);
    ArrayList<? extends DiskLatencyJVMInterface> getSlowDisksReport();
}
