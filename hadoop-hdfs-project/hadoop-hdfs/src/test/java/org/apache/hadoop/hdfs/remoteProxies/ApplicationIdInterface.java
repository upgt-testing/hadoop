package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ApplicationIdInterface {

    int getId();

    long getClusterTimestamp();

    //int compareTo(ApplicationId other);

    String toString();

    int hashCode();

    boolean equals(Object obj);
}
