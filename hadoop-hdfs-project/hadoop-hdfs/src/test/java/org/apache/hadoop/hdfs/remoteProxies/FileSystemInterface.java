package org.apache.hadoop.hdfs.remoteProxies;

import java.net.URI;
import java.util.*;
import java.io.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.AclEntry;
//import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.fs.permission.FsAction;

public interface FileSystemInterface {

    void initialize(URI name, Configuration conf);

    String getScheme();

    URI getUri();

    String getCanonicalServiceName();

    String getName();

    PathInterface makeQualified(Path path);

    TokenInterface getDelegationToken(String renewer);

    FileSystemInterface[] getChildFileSystems();

    DelegationTokenIssuerInterface[] getAdditionalTokenIssuers();

    BlockLocationInterface[] getFileBlockLocations(FileStatusInterface file, long start, long len);

    BlockLocationInterface[] getFileBlockLocations(Path p, long start, long len);

    FsServerDefaultsInterface getServerDefaults();

    FsServerDefaultsInterface getServerDefaults(Path p);

    Path resolvePath(Path p);

    FSDataInputStreamInterface open(Path f, int bufferSize);

    FSDataInputStreamInterface open(Path f);

    FSDataInputStreamInterface open(PathHandleInterface fd);

    FSDataInputStreamInterface open(PathHandle fd, int bufferSize);

    PathHandleInterface getPathHandle(FileStatus stat, Options.HandleOpt opt);

    FSDataOutputStreamInterface create(Path f);

    FSDataOutputStreamInterface create(Path f, boolean overwrite);

    FSDataOutputStreamInterface create(Path f, ProgressableInterface progress);

    FSDataOutputStreamInterface create(Path f, short replication);

    FSDataOutputStreamInterface create(Path f, short replication, Progressable progress);

    FSDataOutputStreamInterface create(Path f, boolean overwrite, int bufferSize);

    FSDataOutputStreamInterface create(Path f, boolean overwrite, int bufferSize, Progressable progress);

    FSDataOutputStreamInterface create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize);

    FSDataOutputStreamInterface create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress);

    FSDataOutputStreamInterface create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress);

    FSDataOutputStreamInterface create(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize, short replication, long blockSize, Progressable progress);

    //FSDataOutputStream create(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize, short replication, long blockSize, Progressable progress, ChecksumOpt checksumOpt);

    FSDataOutputStreamInterface createNonRecursive(Path f, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress);

    FSDataOutputStreamInterface createNonRecursive(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress);

    FSDataOutputStreamInterface createNonRecursive(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize, short replication, long blockSize, Progressable progress);

    boolean createNewFile(Path f);

    FSDataOutputStreamInterface append(Path f);

    FSDataOutputStreamInterface append(Path f, int bufferSize);

    FSDataOutputStreamInterface append(Path f, int bufferSize, Progressable progress);

    void concat(Path trg, Path[] psrcs);

    short getReplication(Path src);

    boolean setReplication(Path src, short replication);

    boolean rename(Path src, Path dst);

    boolean truncate(Path f, long newLength);

    boolean delete(Path f);

    boolean delete(Path f, boolean recursive);

    boolean deleteOnExit(Path f);

    boolean cancelDeleteOnExit(Path f);

    boolean exists(Path f);

    boolean isDirectory(Path f);

    boolean isFile(Path f);

    long getLength(Path f);

    ContentSummaryInterface getContentSummary(Path f);

    QuotaUsageInterface getQuotaUsage(Path f);

    void setQuota(Path src, long namespaceQuota, long storagespaceQuota);

    void setQuotaByStorageType(Path src, StorageType type, long quota);

    FileStatusInterface[] listStatus(Path f);

    RemoteIteratorInterface listCorruptFileBlocks(Path path);

    FileStatusInterface[] listStatus(Path f, PathFilterInterface filter);

    FileStatusInterface[] listStatus(Path[] files);

    FileStatusInterface[] listStatus(Path[] files, PathFilter filter);

    FileStatusInterface[] globStatus(Path pathPattern);

    FileStatusInterface[] globStatus(Path pathPattern, PathFilter filter);

    RemoteIterator<LocatedFileStatus> listLocatedStatus(Path f);

    RemoteIterator<FileStatus> listStatusIterator(Path p);

    RemoteIterator<LocatedFileStatus> listFiles(Path f, boolean recursive);

    Path getHomeDirectory();

    void setWorkingDirectory(Path new_dir);

    Path getWorkingDirectory();

    boolean mkdirs(Path f);

    boolean mkdirs(Path f, FsPermission permission);

    void copyFromLocalFile(Path src, Path dst);

    void moveFromLocalFile(Path[] srcs, Path dst);

    void moveFromLocalFile(Path src, Path dst);

    void copyFromLocalFile(boolean delSrc, Path src, Path dst);

    void copyFromLocalFile(boolean delSrc, boolean overwrite, Path[] srcs, Path dst);

    void copyFromLocalFile(boolean delSrc, boolean overwrite, Path src, Path dst);

    void copyToLocalFile(Path src, Path dst);

    void moveToLocalFile(Path src, Path dst);

    void copyToLocalFile(boolean delSrc, Path src, Path dst);

    void copyToLocalFile(boolean delSrc, Path src, Path dst, boolean useRawLocalFileSystem);

    Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile);

    void completeLocalOutput(Path fsOutputFile, Path tmpLocalFile);

    void close();

    long getUsed();

    long getUsed(Path path);

    long getBlockSize(Path f);

    long getDefaultBlockSize();

    long getDefaultBlockSize(Path f);

    short getDefaultReplication();

    short getDefaultReplication(Path path);

    FileStatusInterface getFileStatus(Path f);

    void msync();

    void access(Path path, FsAction mode);

    void createSymlink(Path target, Path link, boolean createParent);

    FileStatusInterface getFileLinkStatus(Path f);

    boolean supportsSymlinks();

    PathInterface getLinkTarget(Path f);

    FileChecksumInterface getFileChecksum(Path f);

    FileChecksumInterface getFileChecksum(Path f, long length);

    void setVerifyChecksum(boolean verifyChecksum);

    void setWriteChecksum(boolean writeChecksum);

    FsStatusInterface getStatus();

    FsStatusInterface getStatus(Path p);

    void setPermission(Path p, FsPermission permission);

    void setOwner(Path p, String username, String groupname);

    void setTimes(Path p, long mtime, long atime);

    PathInterface createSnapshot(Path path);

    PathInterface createSnapshot(Path path, String snapshotName);

    void renameSnapshot(Path path, String snapshotOldName, String snapshotNewName);

    void deleteSnapshot(Path path, String snapshotName);

    void modifyAclEntries(Path path, List<AclEntry> aclSpec);

    void removeAclEntries(Path path, List<AclEntry> aclSpec);

    void removeDefaultAcl(Path path);

    void removeAcl(Path path);

    void setAcl(Path path, List<AclEntry> aclSpec);

    AclStatusInterface getAclStatus(Path path);

    void setXAttr(Path path, String name, byte[] value);

    void setXAttr(Path path, String name, byte[] value, EnumSet<XAttrSetFlag> flag);

    byte[] getXAttr(Path path, String name);

    Map<String, byte[]> getXAttrs(Path path);

    Map<String, byte[]> getXAttrs(Path path, List<String> names);

    List<String> listXAttrs(Path path);

    void removeXAttr(Path path, String name);

    void satisfyStoragePolicy(Path path);

    void setStoragePolicy(Path src, String policyName);

    void unsetStoragePolicy(Path src);

    BlockStoragePolicySpiInterface getStoragePolicy(Path src);

    Collection<? extends BlockStoragePolicySpi> getAllStoragePolicies();

    PathInterface getTrashRoot(Path path);

    Collection<FileStatus> getTrashRoots(boolean allUsers);

    boolean hasPathCapability(Path path, String capability);

    StorageStatisticsInterface getStorageStatistics();

    FSDataOutputStreamBuilderInterface createFile(Path path);

    FSDataOutputStreamBuilderInterface appendFile(Path path);

    FutureDataInputStreamBuilderInterface openFile(Path path);

    FutureDataInputStreamBuilderInterface openFile(PathHandle pathHandle);

    MultipartUploaderBuilderInterface createMultipartUploader(Path basePath);
}
