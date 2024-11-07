package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockReportOptionsInterface {
    boolean isIncremental();
    java.lang.String toString();
    java.net.InetSocketAddress getNamenodeAddr();
}