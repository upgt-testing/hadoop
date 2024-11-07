package org.apache.hadoop.hdfs.remoteProxies;

public interface SlowPeerTrackerInterface {
//    java.util.Map<java.lang.String, java.util.SortedSet<org.apache.hadoop.hdfs.server.blockmanagement.SlowPeerLatencyWithReportingNode>> getReportsForAllDataNodes();
//    java.util.Collection<org.apache.hadoop.hdfs.server.blockmanagement.SlowPeerJsonReport> getJsonReports(int arg0);
    java.util.List<java.lang.String> getSlowNodes(int arg0);
    boolean isSlowPeerTrackerEnabled();
//    java.util.Set<org.apache.hadoop.hdfs.server.blockmanagement.SlowPeerLatencyWithReportingNode> getReportsForNode(java.lang.String arg0);
//    java.util.SortedSet<org.apache.hadoop.hdfs.server.blockmanagement.SlowPeerLatencyWithReportingNode> filterNodeReports(java.util.concurrent.ConcurrentMap<java.lang.String, org.apache.hadoop.hdfs.server.blockmanagement.SlowPeerTracker.LatencyWithLastReportTime> arg0, long arg1);
    void addReport(java.lang.String arg0, java.lang.String arg1, OutlierMetricsInterface arg2);
    java.lang.String getJson();
    long getReportValidityMs();
}