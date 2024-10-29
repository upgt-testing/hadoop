package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributes;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.fs.permission.FsAction;

public interface FSPermissionCheckerInterface {

    boolean isMemberOfGroup(String group);

    String getUser();

    boolean isSuperUser();

    INodeAttributeProviderInterface getAttributesProvider();

    void checkSuperuserPrivilege();

    void checkPermission(String fsOwner, String supergroup, UserGroupInformation callerUgi, INodeAttributes[] inodeAttrs, INode[] inodes, byte[][] components, int snapshotId, String path, int ancestorIndex, boolean doCheckOwner, FsAction ancestorAccess, FsAction parentAccess, FsAction access, FsAction subAccess, boolean ignoreEmptyDir);

    void checkPermissionWithContext(INodeAttributeProvider.AuthorizationContext authzContext);

    void checkPermission(CachePoolInterface pool, FsAction access);
}
