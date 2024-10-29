package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.io.MD5Hash;

import java.util.*;
import java.io.*;

public interface MD5HashInterface {

    void readFields(DataInput in);

    void write(DataOutput out);

    void set(MD5Hash that);

    byte[] getDigest();

    long halfDigest();

    int quarterDigest();

    boolean equals(Object o);

    int hashCode();

    int compareTo(MD5Hash that);

    String toString();

    void setDigest(String hex);
}
