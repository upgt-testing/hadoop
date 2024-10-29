package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.namenode.INodeFileAttributes;
//import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;

public interface FileDiffListInterface {

    void destroyAndCollectSnapshotBlocks(INode.BlocksMapUpdateInfo collectedBlocks);

    void saveSelf2Snapshot(int latestSnapshotId, INodeFile iNodeFile, INodeFileAttributes snapshotCopy, boolean withBlocks);

    //BlockInfo[] findEarlierSnapshotBlocks(int snapshotId);

    //BlockInfo[] findLaterSnapshotBlocks(int snapshotId);
}
