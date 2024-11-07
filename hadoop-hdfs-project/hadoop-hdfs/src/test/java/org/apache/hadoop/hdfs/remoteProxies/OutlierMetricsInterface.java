package org.apache.hadoop.hdfs.remoteProxies;

public interface OutlierMetricsInterface {
    java.lang.Double getMad();
    boolean equals(java.lang.Object arg0);
    java.lang.Double getUpperLimitLatency();
    int hashCode();
    java.lang.Double getMedian();
    java.lang.Double getActualLatency();
}