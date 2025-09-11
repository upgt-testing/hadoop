package org.apache.hadoop.fs;

import org.apache.hadoop.io.WritableJVMInterface;

public interface FileStatusJVMInterface extends WritableJVMInterface {

    long getLen();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    boolean isEncrypted();

    boolean isFile();

    long getBlockSize();

    java.lang.String getGroup();

    boolean isSnapshotEnabled();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void setPath_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    int hashCode();

    int compareTo(java.lang.Object arg0);

    long getModificationTime();

    org.apache.hadoop.fs.PathJVMInterface getSymlink() throws java.io.IOException;

    int compareTo_bridge(org.apache.hadoop.fs.FileStatusJVMInterface arg0);

    java.lang.String getOwner();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    void setSymlink_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    short getReplication();

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
