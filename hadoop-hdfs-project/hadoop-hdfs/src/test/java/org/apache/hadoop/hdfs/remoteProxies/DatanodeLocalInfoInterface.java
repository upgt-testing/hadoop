package org.apache.hadoop.hdfs.remoteProxies;

public interface DatanodeLocalInfoInterface {
    long getUptime();
    java.lang.String getConfigVersion();
    java.lang.String getSoftwareVersion();
    java.lang.String getDatanodeLocalReport();
}