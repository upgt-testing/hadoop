package org.apache.hadoop.hdfs.server.namenode;

public interface LeaseManagerJVMInterface {
    void setLeasePeriod(long softLimit, long hardLimit);
    void triggerMonitorCheckNow();
}
