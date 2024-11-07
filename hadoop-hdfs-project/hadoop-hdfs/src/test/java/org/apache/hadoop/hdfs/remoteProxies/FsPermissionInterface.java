package org.apache.hadoop.hdfs.remoteProxies;

public interface FsPermissionInterface {
    org.apache.hadoop.fs.permission.FsAction getUserAction();
    org.apache.hadoop.fs.permission.FsAction getOtherAction();
    boolean getErasureCodedBit();
    org.apache.hadoop.fs.permission.FsAction getGroupAction();
    java.lang.String toString();
    FsPermissionInterface getFileDefault();
    short toOctal();
    void setUMask(ConfigurationInterface arg0, FsPermissionInterface arg1);
    FsPermissionInterface createImmutable(short arg0);
    boolean equals(java.lang.Object arg0);
    FsPermissionInterface getCachePoolDefault();
    boolean getStickyBit();
    FsPermissionInterface read(java.io.DataInput arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    FsPermissionInterface getMasked();
    FsPermissionInterface applyUMask(FsPermissionInterface arg0);
    short toExtendedShort();
    FsPermissionInterface getUMask(ConfigurationInterface arg0);
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    FsPermissionInterface valueOf(java.lang.String arg0);
    void fromShort(short arg0);
    FsPermissionInterface getDefault();
    int hashCode();
    void validateObject() throws java.io.InvalidObjectException;
    FsPermissionInterface getUnmasked();
    boolean getAclBit();
    boolean getEncryptedBit();
    FsPermissionInterface getDirDefault();
    short toShort();
    void set(org.apache.hadoop.fs.permission.FsAction arg0, org.apache.hadoop.fs.permission.FsAction arg1, org.apache.hadoop.fs.permission.FsAction arg2, boolean arg3);
}