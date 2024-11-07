package org.apache.hadoop.hdfs.remoteProxies;

public interface SlowPeerReportsInterface {
    boolean equals(java.lang.Object arg0);
    boolean haveSlowPeers();
    java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.protocol.OutlierMetrics> getSlowPeers();
    int hashCode();
    SlowPeerReportsInterface create(java.util.Map<java.lang.String, org.apache.hadoop.hdfs.server.protocol.OutlierMetrics> arg0);
}