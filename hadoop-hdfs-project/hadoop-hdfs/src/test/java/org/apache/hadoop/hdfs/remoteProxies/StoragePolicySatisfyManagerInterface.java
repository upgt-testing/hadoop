package org.apache.hadoop.hdfs.remoteProxies;

public interface StoragePolicySatisfyManagerInterface {
    void addPathId(long arg0);
    void start();
    void clearPathIds();
    void stop();
    boolean isSatisfierRunning();
    void removeAllPathIds();
    java.lang.Long getNextPathId();
    boolean isEnabled();
    void changeModeEvent(org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode arg0);
    org.apache.hadoop.hdfs.protocol.HdfsConstants.StoragePolicySatisfierMode getMode();
    void verifyOutstandingPathQLimit() throws java.io.IOException;
}