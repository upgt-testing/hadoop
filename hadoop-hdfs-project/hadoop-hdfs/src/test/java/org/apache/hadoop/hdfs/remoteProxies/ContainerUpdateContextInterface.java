package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
/**
import org.apache.hadoop.yarn.server.resourcemanager.rmcontainer.RMContainer;
import org.apache.hadoop.yarn.api.records.UpdateContainerRequest;
import org.apache.hadoop.yarn.server.scheduler.SchedulerRequestKey;
import org.apache.hadoop.yarn.service.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerUpdateType;
 **/

public interface ContainerUpdateContextInterface {

    /**
    boolean checkAndAddToOutstandingDecreases(UpdateContainerRequestInterface updateReq, SchedulerNodeInterface schedulerNode, ContainerInterface container);

    boolean checkAndAddToOutstandingIncreases(RMContainer rmContainer, SchedulerNode schedulerNode, UpdateContainerRequest updateRequest);

    void removeFromOutstandingUpdate(SchedulerRequestKey schedulerKey, Container container);

    ContainerId matchContainerToOutstandingIncreaseReq(SchedulerNode node, SchedulerRequestKey schedulerKey, RMContainer rmContainer);

    RMContainer swapContainer(RMContainer tempRMContainer, RMContainer existingRMContainer, ContainerUpdateType updateType);
     **/
}
