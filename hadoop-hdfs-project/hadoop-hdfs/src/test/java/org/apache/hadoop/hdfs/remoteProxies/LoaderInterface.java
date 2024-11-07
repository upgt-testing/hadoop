package org.apache.hadoop.hdfs.remoteProxies;

public interface LoaderInterface {
    INodeInterface loadINodeWithLocalName(boolean arg0, java.io.DataInput arg1, boolean arg2, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg3) throws java.io.IOException;
    java.lang.String getParent(java.lang.String arg0);
    void loadLocalNameINodesWithSnapshot(long arg0, java.io.DataInput arg1, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg2) throws java.io.IOException;
    boolean isRoot(byte[][] arg0);
    void updateBlocksMap(INodeFileInterface arg0);
    INodeInterface loadINodeWithLocalName(boolean arg0, java.io.DataInput arg1, boolean arg2) throws java.io.IOException;
    void loadDirectoryWithSnapshot(java.io.DataInput arg0, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg1) throws java.io.IOException;
    int loadDirectory(java.io.DataInput arg0, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg1) throws java.io.IOException;
    void loadRoot(java.io.DataInput arg0, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg1) throws java.io.IOException;
    org.apache.hadoop.hdfs.server.namenode.INodeFileAttributes loadINodeFileAttributes(java.io.DataInput arg0) throws java.io.IOException;
    void loadLocalNameINodes(long arg0, java.io.DataInput arg1, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg2) throws java.io.IOException;
    void load(java.io.File arg0) throws java.io.IOException;
    org.apache.hadoop.hdfs.server.namenode.INodeDirectoryAttributes loadINodeDirectoryAttributes(java.io.DataInput arg0) throws java.io.IOException;
    MD5HashInterface getLoadedImageMd5();
    INodeDirectoryInterface getParentINodeDirectory(byte[][] arg0) throws java.io.IOException;
    void updateRootAttr(INodeWithAdditionalFieldsInterface arg0);
    SnapshotInterface getSnapshot(java.io.DataInput arg0) throws java.io.IOException;
    void loadFilesUnderConstruction(java.io.DataInput arg0, boolean arg1, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg2) throws java.io.IOException;
    void loadSecretManagerState(java.io.DataInput arg0) throws java.io.IOException;
    int loadChildren(INodeDirectoryInterface arg0, java.io.DataInput arg1, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg2) throws java.io.IOException;
    void checkNotLoaded();
    boolean isParent(byte[][] arg0, byte[][] arg1);
    FSDirectoryInterface getFSDirectoryInLoading();
    INodeInterface loadINode(byte[] arg0, boolean arg1, java.io.DataInput arg2, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg3) throws java.io.IOException;
    long getLoadedImageTxId();
    void checkLoaded();
    void loadCacheManagerState(java.io.DataInput arg0) throws java.io.IOException;
    int getLayoutVersion();
    void loadFullNameINodes(long arg0, java.io.DataInput arg1, org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter arg2) throws java.io.IOException;
    void addToParent(INodeDirectoryInterface arg0, INodeInterface arg1) throws org.apache.hadoop.hdfs.server.namenode.IllegalReservedPathException;
    byte[][] getParent(byte[][] arg0);
}