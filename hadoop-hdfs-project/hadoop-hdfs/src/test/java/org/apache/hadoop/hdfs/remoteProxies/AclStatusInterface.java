package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import java.util.*;
import java.io.*;

public interface AclStatusInterface {

    String getOwner();

    String getGroup();

    boolean isStickyBit();

    List<AclEntryInterface> getEntries();

    FsPermissionInterface getPermission();

    boolean equals(Object o);

    int hashCode();

    String toString();

    FsAction getEffectivePermission(AclEntry entry);

    FsAction getEffectivePermission(AclEntry entry, FsPermission permArg);
}
