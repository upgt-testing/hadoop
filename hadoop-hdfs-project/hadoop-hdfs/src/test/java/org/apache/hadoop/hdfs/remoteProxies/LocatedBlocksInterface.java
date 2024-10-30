package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.LocatedBlock;

import java.util.*;
import java.io.*;

public interface LocatedBlocksInterface {

    List<LocatedBlockInterface> getLocatedBlocks();

    LocatedBlockInterface getLastLocatedBlock();

    boolean isLastBlockComplete();

    LocatedBlockInterface get(int index);

    int locatedBlockCount();

    long getFileLength();

    boolean isUnderConstruction();

    FileEncryptionInfoInterface getFileEncryptionInfo();

    ErasureCodingPolicyInterface getErasureCodingPolicy();

    int findBlock(long offset);

    void insertRange(int blockIdx, List<LocatedBlock> newBlocks);

    String toString();
}
