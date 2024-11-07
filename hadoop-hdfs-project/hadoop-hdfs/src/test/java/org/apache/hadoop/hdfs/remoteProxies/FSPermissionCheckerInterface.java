package org.apache.hadoop.hdfs.remoteProxies;

public interface FSPermissionCheckerInterface {
    void checkPermission(INodesInPathInterface arg0, boolean arg1, org.apache.hadoop.fs.permission.FsAction arg2, org.apache.hadoop.fs.permission.FsAction arg3, org.apache.hadoop.fs.permission.FsAction arg4, org.apache.hadoop.fs.permission.FsAction arg5, boolean arg6) throws org.apache.hadoop.security.AccessControlException;
    void checkPermissionWithContext(AuthorizationContextInterface arg0) throws org.apache.hadoop.security.AccessControlException;
    void checkTraverse(org.apache.hadoop.hdfs.server.namenode.INodeAttributes[] arg0, INodeInterface[] arg1, byte[][] arg2, int arg3) throws org.apache.hadoop.security.AccessControlException, org.apache.hadoop.hdfs.protocol.UnresolvedPathException, org.apache.hadoop.fs.ParentNotDirectoryException;
    org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider.AccessControlEnforcer getAccessControlEnforcer();
    boolean hasAclPermission(org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg0, org.apache.hadoop.fs.permission.FsAction arg1, FsPermissionInterface arg2, AclFeatureInterface arg3);
    java.lang.String getUser();
    void checkOwner(org.apache.hadoop.hdfs.server.namenode.INodeAttributes[] arg0, byte[][] arg1, int arg2) throws org.apache.hadoop.security.AccessControlException;
    boolean isMemberOfGroup(java.lang.String arg0);
    void checkPermission(java.lang.String arg0, java.lang.String arg1, UserGroupInformationInterface arg2, org.apache.hadoop.hdfs.server.namenode.INodeAttributes[] arg3, INodeInterface[] arg4, byte[][] arg5, int arg6, java.lang.String arg7, int arg8, boolean arg9, org.apache.hadoop.fs.permission.FsAction arg10, org.apache.hadoop.fs.permission.FsAction arg11, org.apache.hadoop.fs.permission.FsAction arg12, org.apache.hadoop.fs.permission.FsAction arg13, boolean arg14) throws org.apache.hadoop.security.AccessControlException;
    boolean isSuperUser();
    void checkTraverse(FSPermissionCheckerInterface arg0, INodesInPathInterface arg1, boolean arg2) throws org.apache.hadoop.security.AccessControlException, org.apache.hadoop.hdfs.protocol.UnresolvedPathException, org.apache.hadoop.fs.ParentNotDirectoryException;
    boolean isStickyBitViolated(org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg0, org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg1);
    java.lang.String getPath(byte[][] arg0, int arg1, int arg2);
    INodeAttributeProviderInterface getAttributesProvider();
    void check(org.apache.hadoop.hdfs.server.namenode.INodeAttributes[] arg0, byte[][] arg1, int arg2, org.apache.hadoop.fs.permission.FsAction arg3) throws org.apache.hadoop.security.AccessControlException;
    void checkNotSymlink(INodeInterface arg0, byte[][] arg1, int arg2) throws org.apache.hadoop.hdfs.protocol.UnresolvedPathException;
    void throwStickyBitException(java.lang.String arg0, org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg1, java.lang.String arg2, org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg3) throws org.apache.hadoop.security.AccessControlException;
    void checkSimpleTraverse(INodesInPathInterface arg0) throws org.apache.hadoop.hdfs.protocol.UnresolvedPathException, org.apache.hadoop.fs.ParentNotDirectoryException;
    void checkIsDirectory(INodeInterface arg0, byte[][] arg1, int arg2) throws org.apache.hadoop.hdfs.protocol.UnresolvedPathException, org.apache.hadoop.fs.ParentNotDirectoryException;
    void checkSubAccess(byte[][] arg0, int arg1, INodeInterface arg2, int arg3, org.apache.hadoop.fs.permission.FsAction arg4, boolean arg5) throws org.apache.hadoop.security.AccessControlException;
    void checkPermission(CachePoolInterface arg0, org.apache.hadoop.fs.permission.FsAction arg1) throws org.apache.hadoop.security.AccessControlException;
    void checkSuperuserPrivilege() throws org.apache.hadoop.security.AccessControlException;
    void checkStickyBit(org.apache.hadoop.hdfs.server.namenode.INodeAttributes[] arg0, byte[][] arg1, int arg2) throws org.apache.hadoop.security.AccessControlException;
    java.lang.String toAccessControlString(org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg0, java.lang.String arg1, org.apache.hadoop.fs.permission.FsAction arg2);
    boolean hasPermission(org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg0, org.apache.hadoop.fs.permission.FsAction arg1);
    org.apache.hadoop.hdfs.server.namenode.INodeAttributes getINodeAttrs(byte[][] arg0, int arg1, INodeInterface arg2, int arg3);
    void setOperationType(java.lang.String arg0);
    java.lang.String toAccessControlString(org.apache.hadoop.hdfs.server.namenode.INodeAttributes arg0, java.lang.String arg1, org.apache.hadoop.fs.permission.FsAction arg2, boolean arg3);
    void checkPermission(INodeInterface arg0, int arg1, org.apache.hadoop.fs.permission.FsAction arg2) throws org.apache.hadoop.security.AccessControlException;
}