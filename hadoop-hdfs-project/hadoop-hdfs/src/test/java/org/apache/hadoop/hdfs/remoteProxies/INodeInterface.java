package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.hdfs.server.namenode.*;

public interface INodeInterface {

    long getId();

    String getUserName();

    String getGroupName();

    FsPermissionInterface getFsPermission();

    AclFeatureInterface getAclFeature();

    XAttrFeatureInterface getXAttrFeature();

    INodeAttributesInterface getSnapshotINode(int snapshotId);

    boolean isInCurrentState();

    boolean isInLatestSnapshot(int latestSnapshotId);

    boolean isAncestorDirectory(INodeDirectoryInterface dir);

    boolean shouldRecordInSrcSnapshot(int latestInDst);

    boolean isReference();

    INodeReferenceInterface asReference();

    boolean isFile();

    boolean isSetStoragePolicy();

    INodeFileInterface asFile();

    boolean isDirectory();

    INodeDirectoryInterface asDirectory();

    boolean isSymlink();

    INodeSymlinkInterface asSymlink();

    void cleanSubtree(INode.ReclaimContext reclaimContext, int snapshotId, int priorSnapshotId);

    void destroyAndCollectBlocks(INode.ReclaimContext reclaimContext);

    ContentSummaryInterface computeContentSummary(BlockStoragePolicySuite bsps);

    ContentSummaryInterface computeAndConvertContentSummary(int snapshotId, ContentSummaryComputationContext summary);

    ContentSummaryComputationContextInterface computeContentSummary(int snapshotId, ContentSummaryComputationContext summary);

    void addSpaceConsumed(QuotaCounts counts);

    QuotaCountsInterface getQuotaCounts();

    boolean isQuotaSet();

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuite bsps);

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuite bsps, byte blockStoragePolicyId, boolean useCache, int lastSnapshotId);

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuite bsps, boolean useCache);

    String getLocalName();

    byte[] getKey();

    void setLocalName(byte[] name);

    String getFullPathName();

    boolean isDeleted();

    byte[][] getPathComponents();

    String toString();

    String getObjectString();

    String getParentString();

    String toDetailString();

    INodeDirectoryInterface getParent();

    INodeReferenceInterface getParentReference();

    boolean isLastReference();

    void setParent(INodeDirectory parent);

    void setParentReference(INodeReference parent);

    void clear();

    long getModificationTime();

    INode updateModificationTime(long mtime, int latestSnapshotId);

    void setModificationTime(long modificationTime);

    INode setModificationTime(long modificationTime, int latestSnapshotId);

    long getAccessTime();

    void setAccessTime(long accessTime);

    INode setAccessTime(long accessTime, int latestSnapshotId, boolean skipCaptureAccessTimeOnlyChangeInSnapshot);

    byte getStoragePolicyID();

    byte getLocalStoragePolicyID();

    byte getStoragePolicyIDForQuota(byte parentStoragePolicyId);

    int compareTo(byte[] bytes);

    boolean equals(Object that);

    int hashCode();

    StringBuffer dumpTreeRecursively();

    void dumpTreeRecursively(PrintStream out);

    void dumpTreeRecursively(PrintWriter out, StringBuilder prefix, int snapshotId);
}
