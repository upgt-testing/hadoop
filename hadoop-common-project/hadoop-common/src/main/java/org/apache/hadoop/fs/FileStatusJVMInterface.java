package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileStatusJVMInterface extends WritableJVMInterface {

    int hashCode();

    void setPath_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    long getModificationTime();

    org.apache.hadoop.fs.PathJVMInterface getSymlink() throws java.io.IOException;

    long getLen();

    int compareTo_bridge(org.apache.hadoop.fs.FileStatusJVMInterface arg0);

    java.lang.String getOwner();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    boolean isEncrypted();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    long getBlockSize();

    void setSymlink_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    short getReplication();

    boolean isFile();

    java.lang.String getGroup();

    boolean isDirectory();

    org.apache.hadoop.fs.PathJVMInterface getPath();

    org.apache.hadoop.fs.permission.FsPermissionJVMInterface getPermission();

    long getAccessTime();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    boolean isDir();

    boolean isSymlink();
}
