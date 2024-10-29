package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.protocol.Block;

public interface ReceivedDeletedBlockInfoInterface {

    BlockInterface getBlock();

    void setBlock(Block blk);

    String getDelHints();

    void setDelHints(String hints);

    //BlockStatus getStatus();

    boolean equals(Object o);

    int hashCode();

    boolean blockEquals(Block b);

    boolean isDeletedBlock();

    String toString();
}
