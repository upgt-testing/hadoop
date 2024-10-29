package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.snapshot.FileDiff;

public interface FileWithSnapshotFeatureInterface {

    boolean isCurrentFileDeleted();

    void deleteCurrentFile();

    FileDiffListInterface getDiffs();

    short getMaxBlockRepInDiffs(FileDiffInterface excluded);

    String getDetailedString();

    void cleanFile(INode.ReclaimContext reclaimContext, INodeFile file, int snapshotId, int priorSnapshotId, byte storagePolicyId);

    void clearDiffs();

    void updateQuotaAndCollectBlocks(INode.ReclaimContext reclaimContext, INodeFile file, FileDiff removed);

    void collectBlocksAndClear(INode.ReclaimContext reclaimContext, INodeFile file);

    String toString();
}
