package org.apache.hadoop.hdfs.remoteProxies;

public interface DataTransferThrottlerInterface {
    void throttle(long arg0, CancelerInterface arg1);
    void setBandwidth(long arg0);
    long getBandwidth();
    void throttle(long arg0);
}