package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.Block;

import java.util.*;
import java.io.*;

public interface ExtendedBlockInterface {

    String getBlockPoolId();

    String getBlockName();

    long getNumBytes();

    long getBlockId();

    long getGenerationStamp();

    void setBlockId(long bid);

    void setGenerationStamp(long genStamp);

    void setNumBytes(long len);

    void set(String poolId, Block blk);

    BlockInterface getLocalBlock();

    boolean equals(Object o);

    int hashCode();

    String toString();
}
