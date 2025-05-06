package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.server.namenode.snapshot.SnapshotJVMInterface;

public interface INodeDirectoryJVMInterface {
    long getId();
    DirectoryWithQuotaFeatureJVMInterface getDirectoryWithQuotaFeature();
    boolean isQuotaSet();
    String getFullPathName();
    boolean isWithSnapshot();
    boolean isSnapshottable();
    QuotaCountsJVMInterface getQuotaCounts();
    SnapshotJVMInterface getSnapshot(byte[] snapshotName);
}
