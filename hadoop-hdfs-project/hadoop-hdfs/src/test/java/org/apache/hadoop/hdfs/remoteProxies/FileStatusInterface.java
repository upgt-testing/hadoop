package org.apache.hadoop.hdfs.remoteProxies;

public interface FileStatusInterface {
    boolean isEncrypted();
    void setPath(PathInterface arg0);
    boolean isSymlink();
    long getLen();
    void setOwner(java.lang.String arg0);
    long getBlockSize();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    int hashCode();
    int compareTo(java.lang.Object arg0);
    java.lang.String toString();
    void setSymlink(PathInterface arg0);
    long getAccessTime();
    boolean isFile();
    long getModificationTime();
    short getReplication();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void validateObject() throws java.io.InvalidObjectException;
    boolean isDir();
    void setGroup(java.lang.String arg0);
    java.util.Set<org.apache.hadoop.fs.FileStatus.AttrFlags> attributes(boolean arg0, boolean arg1, boolean arg2, boolean arg3);
    boolean isErasureCoded();
    PathInterface getSymlink() throws java.io.IOException;
    FsPermissionInterface getPermission();
    PathInterface getPath();
    java.lang.String getOwner();
    boolean isDirectory();
    java.lang.String getGroup();
    boolean isSnapshotEnabled();
    int compareTo(FileStatusInterface arg0);
    void setPermission(FsPermissionInterface arg0);
    boolean equals(java.lang.Object arg0);
    boolean hasAcl();
}