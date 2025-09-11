package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileStatusJVMInterface extends WritableJVMInterface {

    long getLen();

    boolean isEncrypted();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    boolean isFile();

    long getBlockSize();

    java.lang.String getGroup();

    boolean isSnapshotEnabled();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void setPath_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    int hashCode();

    long getModificationTime();

    int compareTo(java.lang.Object arg0);

    org.apache.hadoop.fs.PathJVMInterface getSymlink() throws java.io.IOException;

    int compareTo_bridge(org.apache.hadoop.fs.FileStatusJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String getOwner();

    java.lang.String toString();

    short getReplication();

    void setSymlink_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    boolean isDirectory();

    org.apache.hadoop.fs.PathJVMInterface getPath();

    void validateObject() throws java.io.InvalidObjectException;

    boolean hasAcl();

    org.apache.hadoop.fs.permission.FsPermissionJVMInterface getPermission();

    long getAccessTime();

    boolean isErasureCoded();

    boolean isDir();

    boolean isSymlink();
}
