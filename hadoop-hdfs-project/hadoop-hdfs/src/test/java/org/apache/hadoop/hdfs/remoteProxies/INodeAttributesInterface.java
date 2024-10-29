package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.permission.FsPermission;

public interface INodeAttributesInterface {

    boolean isDirectory();

    byte[] getLocalNameBytes();

    String getUserName();

    String getGroupName();

    FsPermission getFsPermission();

    short getFsPermissionShort();

    long getPermissionLong();

    AclFeatureInterface getAclFeature();

    XAttrFeatureInterface getXAttrFeature();

    long getModificationTime();

    long getAccessTime();
}
