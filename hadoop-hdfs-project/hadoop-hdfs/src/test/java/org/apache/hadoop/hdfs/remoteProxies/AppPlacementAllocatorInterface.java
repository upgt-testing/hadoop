package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
/**
import org.apache.hadoop.yarn.server.scheduler.SchedulerRequestKey;
import org.apache.hadoop.yarn.api.records.SchedulingRequest;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.common.PendingAsk;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.common.ContainerRequest;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.NodeType;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.SchedulerNode;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.SchedulingMode;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.AppSchedulingInfo;
 **/

public interface AppPlacementAllocatorInterface {

    /**
    Iterator<N> getPreferredNodeIterator(CandidateNodeSetInterface candidateNodeSet);

    PendingAskUpdateResultInterface updatePendingAsk(Collection<ResourceRequest> requests, boolean recoverPreemptedRequestForAContainer);

    PendingAskUpdateResult updatePendingAsk(SchedulerRequestKey schedulerRequestKey, SchedulingRequest schedulingRequest, boolean recoverPreemptedRequestForAContainer);

    Map<String, ResourceRequest> getResourceRequests();

    PendingAsk getPendingAsk(String resourceName);

    int getOutstandingAsksCount(String resourceName);

    ContainerRequest allocate(SchedulerRequestKey schedulerKey, NodeType type, SchedulerNode node);

    boolean canAllocate(NodeType type, SchedulerNode node);

    boolean canDelayTo(String resourceName);

    boolean precheckNode(SchedulerNode schedulerNode, SchedulingMode schedulingMode, Optional<DiagnosticsCollector> dcOpt);

    boolean precheckNode(SchedulerNode schedulerNode, SchedulingMode schedulingMode);
     **/

    String getPrimaryRequestedNodePartition();

    int getUniqueLocationAsks();

    void showRequests();

    //void initialize(AppSchedulingInfo appSchedulingInfo, SchedulerRequestKey schedulerRequestKey, RMContextInterface rmContext);

    //SchedulingRequest getSchedulingRequest();

    int getPlacementAttempt();

    void incrementPlacementAttempt();
}
