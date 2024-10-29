package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ContainerIdInterface {

    ApplicationAttemptIdInterface getApplicationAttemptId();

    int getId();

    long getContainerId();

    int hashCode();

    boolean equals(Object obj);

    //int compareTo(ContainerId other);

    String toString();
}
