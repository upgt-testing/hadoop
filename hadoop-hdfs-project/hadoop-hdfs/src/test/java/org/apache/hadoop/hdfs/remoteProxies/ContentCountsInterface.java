package org.apache.hadoop.hdfs.remoteProxies;

public interface ContentCountsInterface {
    long[] getTypeSpaces();
    long getFileCount();
    long getSymlinkCount();
    long getDirectoryCount();
    long getSnapshotableDirectoryCount();
    long getLength();
    void addTypeSpace(org.apache.hadoop.fs.StorageType arg0, long arg1);
    long getStoragespace();
    void addContents(ContentCountsInterface arg0);
    void addTypeSpaces(EnumCountersInterface<org.apache.hadoop.fs.StorageType> arg0);
    long getSnapshotCount();
    long getTypeSpace(org.apache.hadoop.fs.StorageType arg0);
    void addContent(org.apache.hadoop.hdfs.server.namenode.Content arg0, long arg1);
}