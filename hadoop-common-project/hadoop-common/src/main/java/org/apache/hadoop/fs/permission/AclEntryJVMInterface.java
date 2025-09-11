package org.apache.hadoop.fs.permission;

public interface AclEntryJVMInterface {

    int hashCode();

    java.lang.Object getPermission();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.lang.String getName();

    java.lang.Object getType();

    java.lang.String toStringStable();

    java.lang.Object getScope();
}
