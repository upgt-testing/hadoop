package org.apache.hadoop.hdfs.server.namenode.snapshot;

import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryJVMInterface;

import java.util.List;

public interface SnapshotManagerJVMInterface {
    int getNumSnapshots();
    int getNumSnapshottableDirs();
    void setAllowNestedSnapshots(boolean allowNestedSnapshots);
    void setCaptureOpenFiles(boolean captureOpenFiles);
    List<? extends INodeDirectoryJVMInterface> getSnapshottableDirs();
}
