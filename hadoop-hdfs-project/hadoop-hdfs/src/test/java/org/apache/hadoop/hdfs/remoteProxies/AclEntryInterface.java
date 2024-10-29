package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.permission.AclEntryScope;
import org.apache.hadoop.fs.permission.AclEntryType;
import org.apache.hadoop.fs.permission.FsAction;

import java.util.*;
import java.io.*;

public interface AclEntryInterface {

    AclEntryType getType();

    String getName();

    FsAction getPermission();

    AclEntryScope getScope();

    boolean equals(Object o);

    int hashCode();

    String toString();

    String toStringStable();
}
