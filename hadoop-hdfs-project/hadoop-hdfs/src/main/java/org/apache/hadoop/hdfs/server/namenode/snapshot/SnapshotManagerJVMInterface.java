package org.apache.hadoop.hdfs.server.namenode.snapshot;

public interface SnapshotManagerJVMInterface {
    int getNumSnapshots();
    int getNumSnapshottableDirs();
    void setAllowNestedSnapshots(boolean allowNestedSnapshots);
    void setCaptureOpenFiles(boolean captureOpenFiles);
}
