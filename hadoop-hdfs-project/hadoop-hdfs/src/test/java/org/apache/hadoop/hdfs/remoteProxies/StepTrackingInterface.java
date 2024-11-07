package org.apache.hadoop.hdfs.remoteProxies;

public interface StepTrackingInterface {
    StepTrackingInterface clone();
    void copy(AbstractTrackingInterface arg0);
}