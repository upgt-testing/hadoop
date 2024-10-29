package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface QueueInterface {

    AbstractUsersManagerInterface getAbstractUsersManager();

    //void recoverContainer(ResourceInterface clusterResource, SchedulerApplicationAttemptInterface schedulerAttempt, RMContainer rmContainer);

    Set<String> getAccessibleNodeLabels();

    String getDefaultNodeLabelExpression();

    //void incPendingResource(String nodeLabel, Resource resourceToInc);

    //void decPendingResource(String nodeLabel, Resource resourceToDec);

    PriorityInterface getDefaultApplicationPriority();

    //void incReservedResource(String partition, Resource reservedRes);

    //void decReservedResource(String partition, Resource reservedRes);
}
