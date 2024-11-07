package org.apache.hadoop.hdfs.remoteProxies;

public interface ProcessingDetailsInterface {
    void set(org.apache.hadoop.ipc.ProcessingDetails.Timing arg0, long arg1);
    long get(org.apache.hadoop.ipc.ProcessingDetails.Timing arg0);
    void add(org.apache.hadoop.ipc.ProcessingDetails.Timing arg0, long arg1, java.util.concurrent.TimeUnit arg2);
    void set(org.apache.hadoop.ipc.ProcessingDetails.Timing arg0, long arg1, java.util.concurrent.TimeUnit arg2);
    java.lang.String toString();
    long get(org.apache.hadoop.ipc.ProcessingDetails.Timing arg0, java.util.concurrent.TimeUnit arg1);
}