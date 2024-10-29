package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.api.records.ContainerId;

public interface SchedulerRequestKeyInterface {

    PriorityInterface getPriority();

    long getAllocationRequestId();

    ContainerIdInterface getContainerToUpdate();

    //int compareTo(SchedulerRequestKey o);

    boolean equals(Object o);

    int hashCode();

    String toString();
}
