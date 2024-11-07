package org.apache.hadoop.hdfs.remoteProxies;

public interface DelegationTokenInformationInterface {
    java.lang.String getTrackingId();
    long getRenewDate();
    byte[] getPassword();
}