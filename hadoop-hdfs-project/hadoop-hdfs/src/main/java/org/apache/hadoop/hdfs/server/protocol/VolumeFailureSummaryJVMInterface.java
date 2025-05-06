package org.apache.hadoop.hdfs.server.protocol;

public interface VolumeFailureSummaryJVMInterface {
    String[] getFailedStorageLocations();
}
