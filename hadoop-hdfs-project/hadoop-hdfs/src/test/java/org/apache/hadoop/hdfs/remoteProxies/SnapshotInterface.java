package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshotInterface {
    int hashCode();
    java.lang.String getSnapshotPath(java.lang.String arg0, java.lang.String arg1);
    int getSnapshotId(SnapshotInterface arg0);
    java.lang.String generateDefaultSnapshotName();
    int compareTo(byte[] arg0);
    boolean equals(java.lang.Object arg0);
    java.lang.String getSnapshotName(SnapshotInterface arg0);
    java.lang.String toString();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    RootInterface getRoot();
    SnapshotInterface read(java.io.DataInput arg0, LoaderInterface arg1) throws java.io.IOException;
    int getId();
    int findLatestSnapshot(INodeInterface arg0, int arg1);
}