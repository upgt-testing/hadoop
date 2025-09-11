package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface ContentSummaryJVMInterface extends QuotaUsageJVMInterface, WritableJVMInterface {

    int hashCode();

    long getLength();

    long getSnapshotDirectoryCount();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    long getSnapshotFileCount();

    long getDirectoryCount();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.String toString(boolean arg0, boolean arg1, boolean arg2);

    java.lang.String toString(boolean arg0, boolean arg1, boolean arg2, boolean arg3, java.util.List<org.apache.hadoop.fs.StorageType> arg4);

    java.lang.String toString(boolean arg0);

    long getSnapshotSpaceConsumed();

    long getFileCount();

    java.lang.String toString(boolean arg0, boolean arg1);

    long getSnapshotLength();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.String toString(boolean arg0, boolean arg1, boolean arg2, java.util.List<org.apache.hadoop.fs.StorageType> arg3);
}
