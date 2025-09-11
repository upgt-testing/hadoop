package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileStatusJVMInterface extends WritableJVMInterface {

    void setPath_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    int hashCode();

    org.apache.hadoop.fs.PathJVMInterface getSymlink() throws java.io.IOException;

    long getModificationTime();

    int compareTo(java.lang.Object arg0);

    long getLen();

    int compareTo_bridge(org.apache.hadoop.fs.FileStatusJVMInterface arg0);

    java.lang.String getOwner();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    boolean isEncrypted();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    void setSymlink_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    short getReplication();

    long getBlockSize();

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
