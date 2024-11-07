package org.apache.hadoop.hdfs.remoteProxies;

public interface HdfsLocatedFileStatusInterface {
    void setGroup(java.lang.String arg0);
    java.util.Set<org.apache.hadoop.fs.FileStatus.AttrFlags> convert(java.util.Set<org.apache.hadoop.hdfs.protocol.HdfsFileStatus.Flags> arg0);
    int hashCode();
    byte[] getLocalNameInBytes();
    LocatedBlocksInterface getLocatedBlocks();
    java.lang.String getGroup();
    FsPermissionInterface getPermission();
    int getChildrenNum();
    java.lang.String getFullName(java.lang.String arg0);
    BlockLocationInterface[] getBlockLocations();
    int compareTo(FileStatusInterface arg0);
    long getLen();
    short getReplication();
    long getBlockSize();
    int compareTo(java.lang.Object arg0);
    LocatedFileStatusInterface makeQualifiedLocated(java.net.URI arg0, PathInterface arg1);
    void setPath(PathInterface arg0);
    java.lang.String getOwner();
    void setBlockLocations(BlockLocationInterface[] arg0);
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    PathInterface getFullPath(PathInterface arg0);
    java.util.Set<org.apache.hadoop.fs.FileStatus.AttrFlags> attributes(boolean arg0, boolean arg1, boolean arg2, boolean arg3);
    PathInterface getSymlink() throws java.io.IOException;
    boolean isErasureCoded();
    ErasureCodingPolicyInterface getErasureCodingPolicy();
    long getFileId();
    void setOwner(java.lang.String arg0);
    long getModificationTime();
    void validateObject() throws java.io.InvalidObjectException;
    boolean isEncrypted();
    boolean isDirectory();
    boolean isFile();
    PathInterface getPath();
    boolean isDir();
    boolean hasAcl();
    long getAccessTime();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    byte getStoragePolicy();
    boolean isSnapshotEnabled();
    FileEncryptionInfoInterface getFileEncryptionInfo();
    void setPermission(FsPermissionInterface arg0);
    boolean isEmptyLocalName();
    FsPermissionInterface convert(boolean arg0, boolean arg1, FsPermissionInterface arg2, java.util.Set<org.apache.hadoop.hdfs.protocol.HdfsFileStatus.Flags> arg3);
    java.lang.String getLocalName();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    FileStatusInterface makeQualified(java.net.URI arg0, PathInterface arg1);
    boolean isSymlink();
    void setSymlink(PathInterface arg0);
    byte[] getSymlinkInBytes();
}