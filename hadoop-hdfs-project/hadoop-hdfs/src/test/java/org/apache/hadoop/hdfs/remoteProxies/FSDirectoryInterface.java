package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.namenode.FSDirectory;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodesInPath;
import org.apache.hadoop.hdfs.server.namenode.XAttrFeature;
//import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;
import org.apache.hadoop.hdfs.util.EnumCounters;
import org.apache.hadoop.fs.permission.FsPermission;

public interface FSDirectoryInterface {

    void setINodeAttributeProvider(INodeAttributeProviderInterface provider);

    int getReadHoldCount();

    int getWriteHoldCount();

    int getListLimit();

    SortedSet<String> getProtectedDirectories();

    boolean isProtectedSubDirectoriesEnable();

    INodeDirectoryInterface getRoot();

    BlockStoragePolicySuite getBlockStoragePolicySuite();

    boolean isPosixAclInheritanceEnabled();

    void setPosixAclInheritanceEnabled(boolean posixAclInheritanceEnabled);

    void close();

    INodesInPathInterface resolvePath(FSPermissionCheckerInterface pc, String src, FSDirectory.DirOp dirOp);

    INodesInPathInterface unprotectedResolvePath(String src);

    boolean isNonEmptyDirectory(INodesInPath inodesInPath);

    void updateCount(INodesInPath iip, INode.QuotaDelta quotaDelta, boolean check);

    //void updateSpaceForCompleteBlock(BlockInfo completeBlk, INodesInPath inodes);

    EnumCounters<StorageType> getStorageTypeDeltas(byte storagePolicyID, long dsDelta, short oldRep, short newRep);

    INodesInPathInterface addLastINode(INodesInPath existing, INode inode, FsPermission modes, boolean checkQuota);

    long removeLastINode(INodesInPath iip);

    long getYieldCount();

    INodeMapInterface getINodeMap();

    void addToInodeMap(INode inode);

    void addRootDirToEncryptionZone(XAttrFeature xaf);

    void removeFromInodeMap(List<? extends INode> inodes);

    INode getInode(long id);

    INodesInPathInterface getINodesInPath(String src, FSDirectory.DirOp dirOp);

    INodesInPathInterface getINodesInPath(byte[][] components, FSDirectory.DirOp dirOp);

    INode getINode(String src);

    INode getINode4Write(String src);

    INode getINode(String src, FSDirectory.DirOp dirOp);

    long getLastInodeId();
}
