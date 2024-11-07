package org.apache.hadoop.hdfs.remoteProxies;

public interface DiskBalancerWorkEntryInterface {
    DiskBalancerWorkItemInterface getWorkItem();
    void setWorkItem(DiskBalancerWorkItemInterface arg0);
    java.lang.String getSourcePath();
    java.lang.String getDestPath();
    void setDestPath(java.lang.String arg0);
    void setSourcePath(java.lang.String arg0);
}