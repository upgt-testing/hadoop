package org.apache.hadoop.hdfs.remoteProxies;

public interface BeanInterface {
    java.lang.String getOwner();
    java.lang.String getGroup();
    java.lang.String getPath();
    int getSnapshotNumber();
    int getSnapshotQuota();
    short getPermission();
    long getModificationTime();
}