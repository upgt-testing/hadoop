package org.apache.hadoop.hdfs.remoteProxies;

public interface TimerInterface {
    long monotonicNowNanos();
    long now();
    long monotonicNow();
}