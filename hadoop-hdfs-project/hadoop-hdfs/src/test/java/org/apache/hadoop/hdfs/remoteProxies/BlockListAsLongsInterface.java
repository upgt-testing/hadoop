package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface BlockListAsLongsInterface {

    void writeTo(OutputStream os);

    int getNumberOfBlocks();

    //ByteString getBlocksBuffer();

    //List<ByteString> getBlocksBuffers();

    long[] getBlockListAsLongs();

    //Iterator<BlockReportReplicaInter> iterator();
}
