package org.apache.hadoop.hdfs.remoteProxies;

public interface MultipartConfigElementInterface {
    int getFileSizeThreshold();
    long getMaxRequestSize();
    java.lang.String getLocation();
    long getMaxFileSize();
}