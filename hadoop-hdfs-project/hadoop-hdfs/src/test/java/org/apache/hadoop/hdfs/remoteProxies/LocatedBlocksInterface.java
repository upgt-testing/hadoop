package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface LocatedBlocksInterface {

    List<LocatedBlock> getLocatedBlocks();

    LocatedBlock getLastLocatedBlock();

    boolean isLastBlockComplete();

    LocatedBlock get(int index);

    int locatedBlockCount();

    long getFileLength();

    boolean isUnderConstruction();

    FileEncryptionInfoInterface getFileEncryptionInfo();

    ErasureCodingPolicyInterface getErasureCodingPolicy();

    int findBlock(long offset);

    void insertRange(int blockIdx, List<LocatedBlock> newBlocks);

    String toString();
}
