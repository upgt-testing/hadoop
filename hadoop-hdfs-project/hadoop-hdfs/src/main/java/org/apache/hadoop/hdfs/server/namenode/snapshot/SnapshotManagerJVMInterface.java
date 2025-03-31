package org.apache.hadoop.hdfs.server.namenode.snapshot;

import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryJVMInterface;

public interface SnapshotManagerJVMInterface {
    int getNumSnapshots();
    int getNumSnapshottableDirs();
    void setAllowNestedSnapshots(boolean allowNestedSnapshots);
    void setCaptureOpenFiles(boolean captureOpenFiles);
    INodeDirectoryJVMInterface[] getSnapshottableDirs();
}
