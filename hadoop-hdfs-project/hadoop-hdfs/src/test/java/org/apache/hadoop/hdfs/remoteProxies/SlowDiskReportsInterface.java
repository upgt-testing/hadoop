package org.apache.hadoop.hdfs.remoteProxies;

public interface SlowDiskReportsInterface {
    java.util.Map<java.lang.String, java.util.Map<org.apache.hadoop.hdfs.server.protocol.SlowDiskReports.DiskOp, java.lang.Double>> getSlowDisks();
    SlowDiskReportsInterface create(java.util.Map<java.lang.String, java.util.Map<org.apache.hadoop.hdfs.server.protocol.SlowDiskReports.DiskOp, java.lang.Double>> arg0);
    boolean haveSlowDisks();
    boolean equals(java.lang.Object arg0);
    int hashCode();
}