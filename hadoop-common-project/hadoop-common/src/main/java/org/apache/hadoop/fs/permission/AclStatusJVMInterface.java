package org.apache.hadoop.fs.permission;

public interface AclStatusJVMInterface {

    int hashCode();

    java.util.List getEntries();

    java.lang.Object getEffectivePermission_bridge(org.apache.hadoop.fs.permission.AclEntryJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1) throws java.lang.IllegalArgumentException;

    org.apache.hadoop.fs.permission.FsPermissionJVMInterface getPermission();

    java.lang.Object getEffectivePermission_bridge(org.apache.hadoop.fs.permission.AclEntryJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String getOwner();

    java.lang.String toString();

    boolean isStickyBit();

    java.lang.String getGroup();
}
