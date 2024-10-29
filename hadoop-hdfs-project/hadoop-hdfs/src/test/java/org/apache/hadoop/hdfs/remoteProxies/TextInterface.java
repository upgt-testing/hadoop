package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.io.Text;

import java.util.*;
import java.io.*;

public interface TextInterface {

    byte[] copyBytes();

    byte[] getBytes();

    int getLength();

    int charAt(int position);

    int find(String what);

    int find(String what, int start);

    void set(String string);

    void set(byte[] utf8);

    void set(Text other);

    void set(byte[] utf8, int start, int len);

    void append(byte[] utf8, int start, int len);

    void clear();

    String toString();

    void readFields(DataInput in);

    void readFields(DataInput in, int maxLength);

    void readWithKnownLength(DataInput in, int len);

    void write(DataOutput out);

    void write(DataOutput out, int maxLength);

    boolean equals(Object o);

    int hashCode();
}
