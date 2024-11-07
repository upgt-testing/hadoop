package org.apache.hadoop.hdfs.remoteProxies;

public interface VolumeFailureSummaryInterface {
    java.lang.String[] getFailedStorageLocations();
    long getLastVolumeFailureDate();
    long getEstimatedCapacityLostTotal();
}