package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.namenode.ContentSummaryComputationContext;
import org.apache.hadoop.hdfs.server.namenode.INode;

public interface INodeReferenceInterface {

    INodeInterface getReferredINode();

    boolean isReference();

    INodeReferenceInterface asReference();

    boolean isFile();

    INodeFileInterface asFile();

    boolean isDirectory();

    INodeDirectoryInterface asDirectory();

    boolean isSymlink();

    INodeSymlinkInterface asSymlink();

    byte[] getLocalNameBytes();

    void setLocalName(byte[] name);

    long getId();

    PermissionStatusInterface getPermissionStatus(int snapshotId);

    String getUserName(int snapshotId);

    String getGroupName(int snapshotId);

    FsPermission getFsPermission(int snapshotId);

    short getFsPermissionShort();

    long getPermissionLong();

    long getModificationTime(int snapshotId);

    INodeInterface updateModificationTime(long mtime, int latestSnapshotId);

    void setModificationTime(long modificationTime);

    long getAccessTime(int snapshotId);

    void setAccessTime(long accessTime);

    byte getStoragePolicyID();

    byte getLocalStoragePolicyID();

    void cleanSubtree(INode.ReclaimContext reclaimContext, int snapshot, int prior);

    void destroyAndCollectBlocks(INode.ReclaimContext reclaimContext);

    ContentSummaryComputationContextInterface computeContentSummary(int snapshotId, ContentSummaryComputationContext summary);

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuite bsps, byte blockStoragePolicyId, boolean useCache, int lastSnapshotId);

    INodeAttributesInterface getSnapshotINode(int snapshotId);

    QuotaCountsInterface getQuotaCounts();

    void clear();

    void dumpTreeRecursively(PrintWriter out, StringBuilder prefix, int snapshot);

    int getDstSnapshotId();
}
