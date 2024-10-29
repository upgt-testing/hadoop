package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.QuotaCounts;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.namenode.ContentCounts;

public interface DirectoryWithSnapshotFeatureInterface {

    int getLastSnapshotId();

    //DirectoryDiffList getDiffs();

    void getSnapshotDirectory(List<INodeDirectory> snapshotDir);

    boolean addChild(INodeDirectory parent, INode inode, boolean setModTime, int latestSnapshotId);

    boolean removeChild(INodeDirectory parent, INode child, int latestSnapshotId);

    ReadOnlyListInterface getChildrenList(INodeDirectory currentINode, int snapshotId);

    INode getChild(INodeDirectory currentINode, byte[] name, int snapshotId);

    INode saveChild2Snapshot(INodeDirectory currentINode, INode child, int latestSnapshotId, INode snapshotCopy);

    void clear(INode.ReclaimContext reclaimContext, INodeDirectory currentINode);

    QuotaCounts computeQuotaUsage4CurrentDirectory(BlockStoragePolicySuite bsps, byte storagePolicyId);

    void computeContentSummary4Snapshot(BlockStoragePolicySuite bsps, ContentCounts counts);

    void cleanDirectory(INode.ReclaimContext reclaimContext, INodeDirectory currentINode, int snapshot, int prior);

    String toString();
}
