package org.apache.hadoop.fs;

public interface ContentSummaryJVMInterface {
    long getDirectoryCount();
    long getFileCount();
    long getLength();
    long getQuota();
    long getSpaceQuota();
    long getSpaceConsumed();
    long getSnapshotDirectoryCount();
    long getSnapshotFileCount();
    long getSnapshotLength();
}
