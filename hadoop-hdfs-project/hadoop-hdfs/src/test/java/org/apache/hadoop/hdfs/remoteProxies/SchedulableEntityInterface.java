package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceUsage;
import org.apache.hadoop.yarn.api.records.Priority;

public interface SchedulableEntityInterface {

    String getId();

    //int compareInputOrderTo(SchedulableEntity other);

    ResourceUsageInterface getSchedulingResourceUsage();

    PriorityInterface getPriority();

    boolean isRecovering();
}
