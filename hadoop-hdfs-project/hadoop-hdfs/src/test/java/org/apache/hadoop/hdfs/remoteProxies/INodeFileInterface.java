package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.ContentSummaryComputationContext;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.QuotaCounts;
//import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;
import org.apache.hadoop.hdfs.server.namenode.snapshot.FileDiffList;
import org.apache.hadoop.hdfs.server.namenode.snapshot.FileWithSnapshotFeature;
import org.apache.hadoop.hdfs.protocol.BlockType;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;

public interface INodeFileInterface {

    boolean isFile();

    INodeFileInterface asFile();

    boolean metadataEquals(INodeFileAttributesInterface other);

    FileUnderConstructionFeatureInterface getFileUnderConstructionFeature();

    boolean isUnderConstruction();

    void setBlock(int index, BlockInfoInterface blk);

    //void convertLastBlockToUC(BlockInfo lastBlock, DatanodeStorageInfoInterface[] locations);

    FileWithSnapshotFeatureInterface addSnapshotFeature(FileDiffList diffs);

    FileWithSnapshotFeature getFileWithSnapshotFeature();

    boolean isWithSnapshot();

    String toDetailString();

    INodeFileAttributesInterface getSnapshotINode(int snapshotId);

    void recordModification(int latestSnapshotId);

    void recordModification(int latestSnapshotId, boolean withBlocks);

    FileDiffList getDiffs();

    short getFileReplication(int snapshot);

    short getFileReplication();

    short getPreferredBlockReplication();

    INodeFileInterface setFileReplication(short replication, int latestSnapshotId);

    long getPreferredBlockSize();

    byte getLocalStoragePolicyID();

    byte getStoragePolicyID();

    void setStoragePolicyID(byte storagePolicyId, int latestSnapshotId);

    byte getErasureCodingPolicyID();

    boolean isStriped();

    BlockType getBlockType();

    long getHeaderLong();

    //BlockInfo[] getBlocks();

    //BlockInfo[] getBlocks(int snapshot);

    void clearBlocks();

    void cleanSubtree(INode.ReclaimContext reclaimContext, int snapshot, int priorSnapshotId);

    void destroyAndCollectBlocks(INode.ReclaimContext reclaimContext);

    void clearFile(INode.ReclaimContext reclaimContext);

    String getName();

    QuotaCountsInterface computeQuotaUsage(BlockStoragePolicySuiteInterface bsps, byte blockStoragePolicyId, boolean useCache, int lastSnapshotId);

    QuotaCountsInterface computeQuotaUsageWithStriped(BlockStoragePolicy bsp, QuotaCounts counts);

    ContentSummaryComputationContextInterface computeContentSummary(int snapshotId, ContentSummaryComputationContext summary);

    long computeFileSize();

    long computeFileSize(int snapshotId);

    long computeFileSizeNotIncludingLastUcBlock();

    long computeFileSize(boolean includesLastUcBlock, boolean usePreferredBlockSize4LastUcBlock);

    QuotaCounts storagespaceConsumed(BlockStoragePolicy bsp);

    QuotaCounts storagespaceConsumedStriped();

    QuotaCounts storagespaceConsumedContiguous(BlockStoragePolicy bsp);

    BlockInfoInterface getLastBlock();

    int numBlocks();

    void dumpTreeRecursively(PrintWriter out, StringBuilder prefix, int snapshotId);

    long collectBlocksBeyondMax(long max, INode.BlocksMapUpdateInfo collectedBlocks, Set<BlockInfo> toRetain);

    //void collectBlocksBeyondSnapshot(BlockInfo[] snapshotBlocks, BlocksMapUpdateInfo collectedBlocks);
}
