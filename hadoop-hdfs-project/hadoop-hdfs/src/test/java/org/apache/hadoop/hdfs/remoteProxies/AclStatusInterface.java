package org.apache.hadoop.hdfs.remoteProxies;

public interface AclStatusInterface {
    org.apache.hadoop.fs.permission.FsAction getEffectivePermission(AclEntryInterface arg0);
    int hashCode();
    FsPermissionInterface getPermission();
    boolean isStickyBit();
    java.lang.String toString();
    java.lang.String getGroup();
    java.lang.String getOwner();
    boolean equals(java.lang.Object arg0);
    java.util.List<org.apache.hadoop.fs.permission.AclEntry> getEntries();
    org.apache.hadoop.fs.permission.FsAction getEffectivePermission(AclEntryInterface arg0, FsPermissionInterface arg1) throws java.lang.IllegalArgumentException;
}