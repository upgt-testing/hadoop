package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.util.ReadOnlyList;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockStoragePolicySuite;
import org.apache.hadoop.hdfs.server.namenode.ContentCounts;

public interface DirectorySnapshottableFeatureInterface {

    int getNumSnapshots();

    SnapshotInterface getSnapshot(byte[] snapshotName);

    Snapshot getSnapshotById(int sid);

    ReadOnlyList<Snapshot> getSnapshotList();

    void renameSnapshot(String path, String oldName, String newName, long mtime);

    int getSnapshotQuota();

    void setSnapshotQuota(int snapshotQuota);

    Snapshot addSnapshot(INodeDirectory snapshotRoot, int id, String name, LeaseManagerInterface leaseManager, boolean captureOpenFiles, int maxSnapshotLimit, long now);

    Snapshot removeSnapshot(INode.ReclaimContext reclaimContext, INodeDirectory snapshotRoot, String snapshotName, long now);

    void computeContentSummary4Snapshot(BlockStoragePolicySuite bsps, ContentCounts counts);

    byte[][] findRenameTargetPath(INodeDirectory snapshotRoot, INodeReference.WithName wn, int snapshotId);

    String toString();

    void dumpTreeRecursively(INodeDirectory snapshotRoot, PrintWriter out, StringBuilder prefix, int snapshot);
}
