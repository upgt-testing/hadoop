package org.apache.hadoop.fs;

import org.apache.hadoop.conf.ConfiguredJVMInterface;

public interface FileSystemJVMInterface extends ConfiguredJVMInterface {

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1, int arg2, short arg3, long arg4, java.lang.Object arg5) throws java.io.IOException;

    boolean isFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void setXAttr_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1, byte[] arg2, java.util.EnumSet<org.apache.hadoop.fs.XAttrSetFlag> arg3) throws java.io.IOException;

    void renameSnapshot_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1, java.lang.String arg2) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface append_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, int arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FsServerDefaultsJVMInterface getServerDefaults_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void copyToLocalFile_bridge(boolean arg0, org.apache.hadoop.fs.PathJVMInterface arg1, org.apache.hadoop.fs.PathJVMInterface arg2, boolean arg3) throws java.io.IOException;

    long getBlockSize_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    long getDefaultBlockSize_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    void setPermission_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1) throws java.io.IOException;

    boolean deleteOnExit_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void initialize_bridge(java.net.URI arg0, org.apache.hadoop.conf.ConfigurationJVMInterface arg1) throws java.io.IOException;

    java.lang.Object listLocatedStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.FileNotFoundException, java.io.IOException;

    long getUsed_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.lang.Object listFiles_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1) throws java.io.FileNotFoundException, java.io.IOException;

    org.apache.hadoop.fs.FileStatusJVMInterface[] listStatus_bridge(org.apache.hadoop.fs.PathJVMInterface[] arg0, java.lang.Object arg1) throws java.io.FileNotFoundException, java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface getTrashRoot_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    boolean mkdirs_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1) throws java.io.IOException;

    org.apache.hadoop.security.token.TokenJVMInterface[] addDelegationTokens_bridge(java.lang.String arg0, org.apache.hadoop.security.CredentialsJVMInterface arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface append_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void concat_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface[] arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface append_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, int arg1, java.lang.Object arg2) throws java.io.IOException;

    short getDefaultReplication();

    org.apache.hadoop.fs.StorageStatisticsJVMInterface getStorageStatistics();

    org.apache.hadoop.fs.FileStatusJVMInterface[] globStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.QuotaUsageJVMInterface getQuotaUsage_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.lang.String getName();

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1, int arg2, short arg3, long arg4) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface getLinkTarget_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.BlockLocationJVMInterface[] getFileBlockLocations_bridge(org.apache.hadoop.fs.FileStatusJVMInterface arg0, long arg1, long arg2) throws java.io.IOException;

    long getLength_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void setWorkingDirectory_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    void removeDefaultAcl_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void completeLocalOutput_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface createNonRecursive_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1, java.util.EnumSet<org.apache.hadoop.fs.CreateFlag> arg2, int arg3, short arg4, long arg5, java.lang.Object arg6) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface createSnapshot_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface getWorkingDirectory();

    java.lang.Object listStatusIterator_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.FileNotFoundException, java.io.IOException;

    void copyFromLocalFile_bridge(boolean arg0, boolean arg1, org.apache.hadoop.fs.PathJVMInterface arg2, org.apache.hadoop.fs.PathJVMInterface arg3) throws java.io.IOException;

    boolean mkdirs_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FileStatusJVMInterface[] listStatus_bridge(org.apache.hadoop.fs.PathJVMInterface[] arg0) throws java.io.FileNotFoundException, java.io.IOException;

    org.apache.hadoop.fs.FSDataInputStreamJVMInterface open_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void removeAcl_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void setOwner_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1, java.lang.String arg2) throws java.io.IOException;

    boolean truncate_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, long arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FsStatusJVMInterface getStatus() throws java.io.IOException;

    long getDefaultBlockSize();

    void copyFromLocalFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface resolvePath_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface createNonRecursive_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1, boolean arg2, int arg3, short arg4, long arg5, java.lang.Object arg6) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, short arg1) throws java.io.IOException;

    void unsetStoragePolicy_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1) throws java.io.IOException;

    void removeAclEntries_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.util.List<org.apache.hadoop.fs.permission.AclEntry> arg1) throws java.io.IOException;

    void removeXAttr_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1) throws java.io.IOException;

    void copyToLocalFile_bridge(boolean arg0, org.apache.hadoop.fs.PathJVMInterface arg1, org.apache.hadoop.fs.PathJVMInterface arg2) throws java.io.IOException;

    org.apache.hadoop.fs.FileStatusJVMInterface[] listStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.FileNotFoundException, java.io.IOException;

    boolean exists_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    boolean rename_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1, java.util.EnumSet<org.apache.hadoop.fs.CreateFlag> arg2, int arg3, short arg4, long arg5, java.lang.Object arg6, java.lang.Object arg7) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamBuilderJVMInterface createFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, short arg1, java.lang.Object arg2) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException;

    org.apache.hadoop.fs.ContentSummaryJVMInterface getContentSummary_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.util.List listXAttrs_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.lang.String getScheme();

    org.apache.hadoop.fs.PathJVMInterface createSnapshot_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FsStatusJVMInterface getStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.util.Collection getTrashRoots(boolean arg0);

    void setWriteChecksum(boolean arg0);

    short getReplication_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.lang.String getCanonicalServiceName();

    org.apache.hadoop.fs.FileSystemJVMInterface[] getChildFileSystems();

    org.apache.hadoop.fs.PathJVMInterface makeQualified_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface createNonRecursive_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1, int arg2, short arg3, long arg4, java.lang.Object arg5) throws java.io.IOException;

    long getUsed() throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface startLocalOutput_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    java.util.Collection getAllStoragePolicies() throws java.io.IOException;

    void createSymlink_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1, boolean arg2) throws org.apache.hadoop.security.AccessControlException, org.apache.hadoop.fs.FileAlreadyExistsException, java.io.FileNotFoundException, org.apache.hadoop.fs.ParentNotDirectoryException, org.apache.hadoop.fs.UnsupportedFileSystemException, java.io.IOException;

    void deleteSnapshot_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1) throws java.io.IOException;

    org.apache.hadoop.fs.BlockLocationJVMInterface[] getFileBlockLocations_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, long arg1, long arg2) throws java.io.IOException;

    void moveFromLocalFile_bridge(org.apache.hadoop.fs.PathJVMInterface[] arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    void msync() throws java.io.IOException, java.lang.UnsupportedOperationException;

    void setTimes_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, long arg1, long arg2) throws java.io.IOException;

    org.apache.hadoop.fs.FileChecksumJVMInterface getFileChecksum_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, long arg1) throws java.io.IOException;

    java.lang.Object getStoragePolicy_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.security.token.TokenJVMInterface getDelegationToken(java.lang.String arg0) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface getHomeDirectory();

    boolean cancelDeleteOnExit_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    java.lang.Object listCorruptFileBlocks_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.permission.AclStatusJVMInterface getAclStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    boolean delete_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1, int arg2, java.lang.Object arg3) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1, boolean arg2, int arg3, short arg4, long arg5, java.lang.Object arg6) throws java.io.IOException;

    java.net.URI getUri();

    boolean delete_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FileStatusJVMInterface[] listStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.Object arg1) throws java.io.FileNotFoundException, java.io.IOException;

    void access_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.Object arg1) throws org.apache.hadoop.security.AccessControlException, java.io.FileNotFoundException, java.io.IOException;

    byte[] getXAttr_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamBuilderJVMInterface appendFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, boolean arg1, int arg2) throws java.io.IOException;

    short getDefaultReplication_bridge(org.apache.hadoop.fs.PathJVMInterface arg0);

    boolean createNewFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    java.util.Map getXAttrs_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.util.List<java.lang.String> arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FileChecksumJVMInterface getFileChecksum_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    void moveFromLocalFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    void copyFromLocalFile_bridge(boolean arg0, org.apache.hadoop.fs.PathJVMInterface arg1, org.apache.hadoop.fs.PathJVMInterface arg2) throws java.io.IOException;

    void close() throws java.io.IOException;

    void setStoragePolicy_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FileStatusJVMInterface[] globStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.Object arg1) throws java.io.IOException;

    org.apache.hadoop.fs.FileStatusJVMInterface getFileStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataOutputStreamJVMInterface create_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.permission.FsPermissionJVMInterface arg1, java.util.EnumSet<org.apache.hadoop.fs.CreateFlag> arg2, int arg3, short arg4, long arg5, java.lang.Object arg6) throws java.io.IOException;

    void copyFromLocalFile_bridge(boolean arg0, boolean arg1, org.apache.hadoop.fs.PathJVMInterface[] arg2, org.apache.hadoop.fs.PathJVMInterface arg3) throws java.io.IOException;

    boolean isDirectory_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;

    boolean setReplication_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, short arg1) throws java.io.IOException;

    void moveToLocalFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    void copyToLocalFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1) throws java.io.IOException;

    void setAcl_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.util.List<org.apache.hadoop.fs.permission.AclEntry> arg1) throws java.io.IOException;

    void setVerifyChecksum(boolean arg0);

    org.apache.hadoop.fs.FileStatusJVMInterface getFileLinkStatus_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws org.apache.hadoop.security.AccessControlException, java.io.FileNotFoundException, org.apache.hadoop.fs.UnsupportedFileSystemException, java.io.IOException;

    org.apache.hadoop.fs.FsServerDefaultsJVMInterface getServerDefaults() throws java.io.IOException;

    void modifyAclEntries_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.util.List<org.apache.hadoop.fs.permission.AclEntry> arg1) throws java.io.IOException;

    void setXAttr_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, java.lang.String arg1, byte[] arg2) throws java.io.IOException;

    org.apache.hadoop.fs.FSDataInputStreamJVMInterface open_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, int arg1) throws java.io.IOException;

    boolean supportsSymlinks();

    java.util.Map getXAttrs_bridge(org.apache.hadoop.fs.PathJVMInterface arg0) throws java.io.IOException;
}
