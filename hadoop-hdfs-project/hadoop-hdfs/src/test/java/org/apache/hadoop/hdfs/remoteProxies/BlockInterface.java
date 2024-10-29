package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.Block;

import java.util.*;
import java.io.*;

public interface BlockInterface {

    void set(long blkid, long len, long genStamp);

    long getBlockId();

    void setBlockId(long bid);

    String getBlockName();

    long getNumBytes();

    void setNumBytes(long len);

    long getGenerationStamp();

    void setGenerationStamp(long stamp);

    String toString();

    void appendStringTo(StringBuilder sb);

    void write(DataOutput out);

    void readFields(DataInput in);

    void writeId(DataOutput out);

    void readId(DataInput in);

    int compareTo(Block b);

    boolean equals(Object obj);

    int hashCode();
}
