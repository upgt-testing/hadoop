package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.io.LongWritable;

import java.util.*;
import java.io.*;

public interface LongWritableInterface {

    void set(long value);

    long get();

    void readFields(DataInput in);

    void write(DataOutput out);

    boolean equals(Object o);

    int hashCode();

    int compareTo(LongWritable o);

    String toString();
}
