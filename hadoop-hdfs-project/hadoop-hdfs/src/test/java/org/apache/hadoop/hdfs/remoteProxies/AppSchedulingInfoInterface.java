package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface AppSchedulingInfoInterface {

    ApplicationIdInterface getApplicationId();

    ApplicationAttemptIdInterface getApplicationAttemptId();

    String getUser();

    long getNewContainerId();

    String getQueueName();

    boolean isPending();

    Set<String> getRequestedPartitions();

    ContainerUpdateContextInterface getUpdateContext();

    //boolean updateResourceRequests(List<ResourceRequest> resourceRequests, boolean recoverPreemptedRequestForAContainer);

    //boolean updateResourceRequests(Map<SchedulerRequestKey, Map<String, ResourceRequest>> dedupRequests, boolean recoverPreemptedRequestForAContainer);

    //boolean updateSchedulingRequests(List<SchedulingRequest> schedulingRequests, boolean recoverPreemptedRequestForAContainer);

    //void removeAppPlacement(SchedulerRequestKey schedulerRequestKey);

    void addRequestedPartition(String partition);

    //void decPendingResource(String partition, Resource toDecrease);

    void updatePlacesBlacklistedByApp(List<String> blacklistAdditions, List<String> blacklistRemovals);

    void updatePlacesBlacklistedBySystem(List<String> blacklistAdditions, List<String> blacklistRemovals);

    boolean getAndResetBlacklistChanged();

    //Collection<SchedulerRequestKey> getSchedulerKeys();

    //List<ResourceRequest> getAllResourceRequests();

    //List<SchedulingRequest> getAllSchedulingRequests();

    //List<RejectedSchedulingRequest> getRejectedRequest();

    PendingAskInterface getNextPendingAsk();

    //PendingAskInterface getPendingAsk(SchedulerRequestKey schedulerKey);

    //PendingAskInterface getPendingAsk(SchedulerRequestKey schedulerKey, String resourceName);

    boolean isPlaceBlacklisted(String resourceName, boolean blacklistedBySystem);

    //ContainerRequestInterface allocate(NodeType type, SchedulerNode node, SchedulerRequestKey schedulerKey, RMContainer containerAllocated);

    void checkForDeactivation();

    void move(Queue newQueue);

    void stop();

    void setQueue(Queue queue);

    Set<String> getBlackListCopy();

    //void transferStateFromPreviousAppSchedulingInfo(AppSchedulingInfo appInfo);

    //void recoverContainer(RMContainer rmContainer, String partition);

    //boolean checkAllocation(NodeType type, SchedulerNode node, SchedulerRequestKey schedulerKey);

    //AppPlacementAllocatorInterface getAppPlacementAllocator(SchedulerRequestKey schedulerkey);

    //boolean canDelayTo(SchedulerRequestKey schedulerKey, String resourceName);

    //boolean precheckNode(SchedulerRequestKey schedulerKey, SchedulerNode schedulerNode, SchedulingMode schedulingMode, Optional<DiagnosticsCollector> dcOpt);

    Map<String, String> getApplicationSchedulingEnvs();

    String getDefaultNodeLabelExpression();

    //RMContext getRMContext();
}
