package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotManagerProxy {
    void setAllowNestedSnapshots(boolean allowNestedSnapshots);
    int getNumSnapshottableDirs();
    int getNumSnapshots();
}
