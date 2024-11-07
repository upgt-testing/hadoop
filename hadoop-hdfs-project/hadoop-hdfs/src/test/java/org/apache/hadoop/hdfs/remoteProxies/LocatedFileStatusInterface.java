package org.apache.hadoop.hdfs.remoteProxies;

public interface LocatedFileStatusInterface {
    java.lang.String getOwner();
    java.lang.String getGroup();
    long getModificationTime();
    boolean isDir();
    long getLen();
    BlockLocationInterface[] getBlockLocations();
    PathInterface getSymlink() throws java.io.IOException;
    boolean isSymlink();
    boolean hasAcl();
    void setPath(PathInterface arg0);
    void setSymlink(PathInterface arg0);
    long getAccessTime();
    void setOwner(java.lang.String arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    java.util.Set<org.apache.hadoop.fs.FileStatus.AttrFlags> attributes(boolean arg0, boolean arg1, boolean arg2, boolean arg3);
    boolean equals(java.lang.Object arg0);
    void setPermission(FsPermissionInterface arg0);
    java.lang.String toString();
    FsPermissionInterface getPermission();
    long getBlockSize();
    void setBlockLocations(BlockLocationInterface[] arg0);
    void setGroup(java.lang.String arg0);
    short getReplication();
    int hashCode();
    boolean isDirectory();
    boolean isSnapshotEnabled();
    boolean isFile();
    int compareTo(java.lang.Object arg0);
    void validateObject() throws java.io.InvalidObjectException;
    boolean isErasureCoded();
    int compareTo(FileStatusInterface arg0);
    PathInterface getPath();
    boolean isEncrypted();
}