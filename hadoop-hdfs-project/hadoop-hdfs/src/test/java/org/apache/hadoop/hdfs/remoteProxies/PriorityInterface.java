package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface PriorityInterface {

    int getPriority();

    void setPriority(int priority);

    int hashCode();

    boolean equals(Object obj);

    //int compareTo(Priority other);

    String toString();
}
