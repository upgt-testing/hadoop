package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileStatusJVMInterface extends WritableJVMInterface {

    long getLen();

    boolean isEncrypted();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    long getBlockSize();

    boolean isFile();

    java.lang.String getGroup();

    boolean isSnapshotEnabled();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    int hashCode();

    void setPath_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    org.apache.hadoop.fs.PathJVMInterface getSymlink() throws java.io.IOException;

    long getModificationTime();

    int compareTo(java.lang.Object arg0);

    int compareTo_bridge(org.apache.hadoop.fs.FileStatusJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String getOwner();

    java.lang.String toString();

    void setSymlink_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    short getReplication();

    boolean isDirectory();

    void validateObject() throws java.io.InvalidObjectException;

    org.apache.hadoop.fs.PathJVMInterface getPath();

    boolean hasAcl();

    org.apache.hadoop.fs.permission.FsPermissionJVMInterface getPermission();

    long getAccessTime();

    boolean isDir();

    boolean isErasureCoded();

    boolean isSymlink();
}
