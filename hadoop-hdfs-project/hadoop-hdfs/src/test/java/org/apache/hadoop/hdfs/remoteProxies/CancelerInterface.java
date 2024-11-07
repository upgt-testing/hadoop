package org.apache.hadoop.hdfs.remoteProxies;

public interface CancelerInterface {
    boolean isCancelled();
    void cancel(java.lang.String arg0);
    java.lang.String getCancellationReason();
}