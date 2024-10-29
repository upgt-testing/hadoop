package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.*;
import org.apache.hadoop.hdfs.server.namenode.snapshot.DirectoryWithSnapshotFeature;
import org.apache.hadoop.hdfs.server.namenode.snapshot.Snapshot;
import org.apache.hadoop.hdfs.util.ReadOnlyList;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;

public interface INodeDirectoryInterface {

    boolean isDirectory();

    INodeDirectoryInterface asDirectory();

    byte getLocalStoragePolicyID();

    byte getStoragePolicyID();

    QuotaCountsInterface getQuotaCounts();

    void addSpaceConsumed(QuotaCounts counts);

    DirectoryWithQuotaFeatureInterface getDirectoryWithQuotaFeature();

    DirectoryWithSnapshotFeatureInterface addSnapshotFeature(DirectoryWithSnapshotFeature.DirectoryDiffList diffs);

    DirectoryWithSnapshotFeatureInterface getDirectoryWithSnapshotFeature();

    boolean isWithSnapshot();

    DirectoryWithSnapshotFeature.DirectoryDiffList getDiffs();

    INodeDirectoryAttributesInterface getSnapshotINode(int snapshotId);

    String toDetailString();

    DirectorySnapshottableFeatureInterface getDirectorySnapshottableFeature();

    boolean isSnapshottable();

    boolean isDescendantOfSnapshotRoot(INodeDirectory snapshotRootDir);

    SnapshotInterface getSnapshot(byte[] snapshotName);

    void setSnapshotQuota(int snapshotQuota);

    SnapshotInterface addSnapshot(int id, String name, LeaseManager leaseManager, boolean captureOpenFiles, int maxSnapshotLimit, long mtime);

    SnapshotInterface removeSnapshot(INode.ReclaimContext reclaimContext, String snapshotName, long mtime);

    void renameSnapshot(String path, String oldName, String newName, long mtime);

    void addSnapshottableFeature();

    void removeSnapshottableFeature();

    void replaceChild(INode oldChild, INode newChild, INodeMapInterface inodeMap);

    void recordModification(int latestSnapshotId);

    INodeInterface saveChild2Snapshot(INode child, int latestSnapshotId, INode snapshotCopy);

    INodeInterface getChild(byte[] name, int snapshotId);

    int searchChild(INode inode);

    ReadOnlyList<INode> getChildrenList(int snapshotId);

    boolean removeChild(INode child, int latestSnapshotId);

    boolean removeChild(INode child);

    boolean addChild(INode node, boolean setModTime, int latestSnapshotId);

    boolean addChild(INode node);

    boolean addChildAtLoading(INode node);

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuite bsps, byte blockStoragePolicyId, boolean useCache, int lastSnapshotId);

    QuotaCountsInterface computeQuotaUsage4CurrentDirectory(BlockStoragePolicySuite bsps, byte storagePolicyId, QuotaCounts counts);

    ContentSummaryComputationContextInterface computeContentSummary(int snapshotId, ContentSummaryComputationContext summary);

    void undoRename4ScrParent(INodeReferenceInterface oldChild, INode newChild);

    void undoRename4DstParent(BlockStoragePolicySuite bsps, INode deletedChild, int latestSnapshotId);

    void clearChildren();

    void clear();

    //void cleanSubtreeRecursively(ReclaimContext reclaimContext, int snapshot, int prior, Map<INode, INode> excludedNodes);

    //void destroyAndCollectBlocks(ReclaimContext reclaimContext);

    //void cleanSubtree(ReclaimContext reclaimContext, int snapshotId, int priorSnapshotId);

    boolean metadataEquals(INodeDirectoryAttributes other);

    void dumpTreeRecursively(PrintWriter out, StringBuilder prefix, int snapshot);

    int getChildrenNum(int snapshotId);
}
