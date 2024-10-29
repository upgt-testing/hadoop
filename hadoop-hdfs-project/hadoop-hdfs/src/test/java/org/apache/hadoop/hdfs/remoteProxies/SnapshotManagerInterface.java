package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.*;

public interface SnapshotManagerInterface {

    boolean getSkipCaptureAccessTimeOnlyChange();

    boolean isAllowNestedSnapshots();

    void setSnapshottable(String path, boolean checkNestedSnapshottable);

    void addSnapshottable(INodeDirectory dir);

    void removeSnapshottable(List<INodeDirectory> toRemove);

    void resetSnapshottable(String path);

    INodeDirectory getSnapshottableRoot(INodesInPath iip);

    INodeDirectory getSnapshottableAncestorDir(INodesInPath iip);

    boolean isDescendantOfSnapshotRoot(INodeDirectory dir);

    String createSnapshot(LeaseManager leaseManager, INodesInPath iip, String snapshotRoot, String snapshotName, long mtime);

    void deleteSnapshot(INodesInPath iip, String snapshotName, INode.ReclaimContext reclaimContext, long now);

    void renameSnapshot(INodesInPath iip, String snapshotRoot, String oldSnapshotName, String newSnapshotName, long now);

    int getNumSnapshottableDirs();

    int getNumSnapshots();

    void write(DataOutput out);

    Map<Integer, SnapshotInterface> read(DataInput in, FSImageFormat.Loader loader);

    SnapshottableDirectoryStatusInterface[] getSnapshottableDirListing(String userName);

    SnapshotDiffReportInterface diff(INodesInPath iip, String snapshotPath, String from, String to);

    SnapshotDiffReportListingInterface diff(INodesInPath iip, String snapshotPath, String from, String to, byte[] startPath, int index, int snapshotDiffReportLimit);

    void clearSnapshottableDirs();

    int getMaxSnapshotID();

    void registerMXBean();

    void shutdown();

    //SnapshottableDirectoryStatus.Bean[] getSnapshottableDirectories();

    //SnapshotInfo.Bean[] getSnapshots();
}
