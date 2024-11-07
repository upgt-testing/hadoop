package org.apache.hadoop.hdfs.remoteProxies;

public interface DataNodePeerMetricsInterface {
    OutlierDetectorInterface getSlowNodeDetector();
    long getMinOutlierDetectionSamples();
    DataNodePeerMetricsInterface create(java.lang.String arg0, ConfigurationInterface arg1);
    java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.protocol.OutlierMetrics> getOutliers();
    void addSendPacketDownstream(java.lang.String arg0, long arg1);
    java.lang.String name();
    MutableRollingAveragesInterface getSendPacketDownstreamRollingAverages();
    void setMinOutlierDetectionNodes(long arg0);
    void setTestOutliers(java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.protocol.OutlierMetrics> arg0);
    long getMinOutlierDetectionNodes();
    void setLowThresholdMs(long arg0);
    long getLowThresholdMs();
    void collectThreadLocalStates();
    java.lang.String dumpSendPacketDownstreamAvgInfoAsJson();
    void setMinOutlierDetectionSamples(long arg0);
}