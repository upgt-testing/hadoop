package org.apache.hadoop.hdfs.remoteProxies;

public interface HAServiceStatusInterface {
    boolean isReadyToBecomeActive();
    org.apache.hadoop.ha.HAServiceProtocol.HAServiceState getState();
    java.lang.String getNotReadyReason();
    HAServiceStatusInterface setNotReadyToBecomeActive(java.lang.String arg0);
    HAServiceStatusInterface setReadyToBecomeActive();
}