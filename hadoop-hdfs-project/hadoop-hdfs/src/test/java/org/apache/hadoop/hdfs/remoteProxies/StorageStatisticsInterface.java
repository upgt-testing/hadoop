package org.apache.hadoop.hdfs.remoteProxies;

public interface StorageStatisticsInterface {
    java.lang.String getName();
    java.lang.Long getLong(java.lang.String arg0);
    boolean isTracked(java.lang.String arg0);
    java.lang.String getScheme();
    java.util.Iterator<org.apache.hadoop.fs.StorageStatistics.LongStatistic> getLongStatistics();
    void reset();
}