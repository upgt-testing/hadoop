package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ApplicationAttemptIdInterface {

    //ApplicationId getApplicationId();

    int getAttemptId();

    int hashCode();

    boolean equals(Object obj);

    //int compareTo(ApplicationAttemptId other);

    String toString();
}
