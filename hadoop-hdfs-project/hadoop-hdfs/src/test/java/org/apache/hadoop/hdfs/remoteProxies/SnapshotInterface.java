package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface SnapshotInterface {

    int getId();

    //Root getRoot();

    int compareTo(byte[] bytes);

    boolean equals(Object that);

    int hashCode();

    String toString();
}
