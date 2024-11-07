package org.apache.hadoop.hdfs.remoteProxies;

public interface SnapshottableDirectoryStatusInterface {
    int getSnapshotNumber();
    byte[] getParentFullPath();
    int maxLength(int arg0, java.lang.Object arg1);
    org.apache.hadoop.hdfs.protocol.HdfsFileStatus getDirStatus();
    void print(SnapshottableDirectoryStatusInterface[] arg0, java.io.PrintStream arg1);
    int getSnapshotQuota();
    PathInterface getFullPath();
}