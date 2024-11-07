package org.apache.hadoop.hdfs.remoteProxies;

public interface ReconfigurationTaskStatusInterface {
    long getEndTime();
    java.util.Map<org.apache.hadoop.conf.ReconfigurationUtil.PropertyChange, java.util.Optional<java.lang.String>> getStatus();
    boolean stopped();
    boolean hasTask();
    long getStartTime();
}