package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.INode;
//import org.apache.hadoop.tools.dynamometer.blockgenerator.BlockInfo;

public interface FileDiffInterface {

    long getFileSize();

    //void setBlocks(BlockInfo[] blocks);

    //BlockInfo[] getBlocks();

    String toString();

    void destroyAndCollectSnapshotBlocks(INode.BlocksMapUpdateInfo collectedBlocks);
}
