package org.apache.hadoop.hdfs.remoteProxies;

public interface AclEntryInterface {
    org.apache.hadoop.fs.permission.FsAction getPermission();
    int hashCode();
    boolean equals(java.lang.Object arg0);
    java.lang.String getName();
    org.apache.hadoop.fs.permission.AclEntryScope getScope();
    java.lang.String toString();
    java.lang.String toStringStable();
    org.apache.hadoop.fs.permission.AclEntryType getType();
    java.lang.String aclSpecToString(java.util.List<org.apache.hadoop.fs.permission.AclEntry> arg0);
    java.util.List<org.apache.hadoop.fs.permission.AclEntry> parseAclSpec(java.lang.String arg0, boolean arg1);
    AclEntryInterface parseAclEntry(java.lang.String arg0, boolean arg1);
}