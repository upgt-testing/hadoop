package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ContainerQueuingLimitInterface {

    int getMaxQueueLength();

    void setMaxQueueLength(int queueLength);

    int getMaxQueueWaitTimeInMs();

    void setMaxQueueWaitTimeInMs(int waitTime);
}
