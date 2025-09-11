package org.apache.hadoop.fs.permission;

public interface AclEntryJVMInterface {

    int hashCode();

    boolean equals(java.lang.Object arg0);

    java.lang.Object getPermission();

    java.lang.String toString();

    java.lang.String getName();

    java.lang.Object getType();

    java.lang.Object getScope();

    java.lang.String toStringStable();
}
