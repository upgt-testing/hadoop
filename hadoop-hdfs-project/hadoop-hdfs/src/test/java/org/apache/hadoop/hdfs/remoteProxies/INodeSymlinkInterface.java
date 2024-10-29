package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.namenode.AclFeature;
import org.apache.hadoop.hdfs.server.namenode.ContentSummaryComputationContext;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.XAttrFeature;

public interface INodeSymlinkInterface {

    boolean isSymlink();

    INodeSymlinkInterface asSymlink();

    String getSymlinkString();

    byte[] getSymlink();

    void cleanSubtree(INode.ReclaimContext reclaimContext, int snapshotId, int priorSnapshotId);

    void destroyAndCollectBlocks(INode.ReclaimContext reclaimContext);

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuite bsps, byte blockStoragePolicyId, boolean useCache, int lastSnapshotId);

    ContentSummaryComputationContextInterface computeContentSummary(int snapshotId, ContentSummaryComputationContext summary);

    void dumpTreeRecursively(PrintWriter out, StringBuilder prefix, int snapshot);

    void removeAclFeature();

    void addAclFeature(AclFeature f);

    void removeXAttrFeature();

    void addXAttrFeature(XAttrFeature f);

    byte getStoragePolicyID();

    byte getLocalStoragePolicyID();
}
