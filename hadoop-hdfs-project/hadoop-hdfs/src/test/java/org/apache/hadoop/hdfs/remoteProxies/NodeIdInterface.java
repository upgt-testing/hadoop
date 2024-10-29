package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface NodeIdInterface {

    String getHost();

    int getPort();

    String toString();

    int hashCode();

    boolean equals(Object obj);

    //int compareTo(NodeId other);
}
