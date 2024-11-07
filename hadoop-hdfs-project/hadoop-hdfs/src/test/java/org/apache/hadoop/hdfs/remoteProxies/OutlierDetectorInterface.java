package org.apache.hadoop.hdfs.remoteProxies;

public interface OutlierDetectorInterface {
    java.util.Map<java.lang.String, java.lang.Double> getOutliers(java.util.Map<java.lang.String, java.lang.Double> arg0);
    java.lang.Double computeMedian(java.util.List<java.lang.Double> arg0);
    void setMinNumResources(long arg0);
    long getMinOutlierDetectionNodes();
    void setLowThresholdMs(long arg0);
    long getLowThresholdMs();
    java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.protocol.OutlierMetrics> getOutlierMetrics(java.util.Map<java.lang.String, java.lang.Double> arg0);
    java.lang.Double computeMad(java.util.List<java.lang.Double> arg0);
}