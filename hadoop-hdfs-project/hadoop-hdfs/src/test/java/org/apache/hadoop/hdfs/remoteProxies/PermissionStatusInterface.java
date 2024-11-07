package org.apache.hadoop.hdfs.remoteProxies;

public interface PermissionStatusInterface {
    java.lang.String getGroupName();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    PermissionStatusInterface createImmutable(java.lang.String arg0, java.lang.String arg1, FsPermissionInterface arg2);
    FsPermissionInterface getPermission();
    PermissionStatusInterface read(java.io.DataInput arg0) throws java.io.IOException;
    java.lang.String getUserName();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    java.lang.String toString();
    void write(java.io.DataOutput arg0, java.lang.String arg1, java.lang.String arg2, FsPermissionInterface arg3) throws java.io.IOException;
}