package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
/**
import org.apache.hadoop.yarn.server.scheduler.OpportunisticContainerContext;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.common.PendingAsk;
import org.apache.hadoop.yarn.server.scheduler.SchedulerRequestKey;
import org.apache.hadoop.yarn.service.api.records.Resource;
import org.apache.hadoop.yarn.server.resourcemanager.rmcontainer.RMContainer;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.common.ContainerRequest;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptState;
import org.apache.hadoop.yarn.service.api.records.Container;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.SchedulingMode;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.placement.AppPlacementAllocator;
 **/

public interface SchedulerApplicationAttemptInterface {

    void setOpportunisticContainerContext(OpportunisticContainerContextInterface oppContext);

    OpportunisticContainerContextInterface getOpportunisticContainerContext();

    Collection<RMContainerInterface> getLiveContainers();

    AppSchedulingInfoInterface getAppSchedulingInfo();

    ContainerUpdateContextInterface getUpdateContext();

    boolean isPending();

    ApplicationAttemptIdInterface getApplicationAttemptId();

    ApplicationIdInterface getApplicationId();

    String getUser();

    Set<ContainerIdInterface> getPendingRelease();

    long getNewContainerId();

    Collection<SchedulerRequestKeyInterface> getSchedulerKeys();

    //PendingAskInterface getPendingAsk(SchedulerRequestKey schedulerKey, String resourceName);

    //int getOutstandingAsksCount(SchedulerRequestKey schedulerKey);

    //int getOutstandingAsksCount(SchedulerRequestKey schedulerKey, String resourceName);

    String getQueueName();

    ResourceInterface getAMResource();

    ResourceInterface getAMResource(String label);

    //void setAMResource(Resource amResource);

    //void setAMResource(String label, Resource amResource);

    boolean isAmRunning();

    void setAmRunning(boolean bool);

    boolean getUnmanagedAM();

    //RMContainerInterface getRMContainer(ContainerId id);

    //void addRMContainer(ContainerId id, RMContainer rmContainer);

    //void removeRMContainer(ContainerId containerId);

    //int getReReservations(SchedulerRequestKey schedulerKey);

    //Resource getCurrentReservation();

    Queue getQueue();


    //boolean updateResourceRequests(List<ResourceRequest> requests);

    //boolean updateSchedulingRequests(List<SchedulingRequest> requests);

    //void recoverResourceRequestsForContainer(ContainerRequest containerRequest);

    //void stop(RMAppAttemptState rmAppAttemptFinalState);

    boolean isStopped();

    //List<RMContainer> getReservedContainers();

    //boolean reserveIncreasedContainer(SchedulerNode node, SchedulerRequestKey schedulerKey, RMContainer rmContainer, Resource reservedResource);

    //RMContainer reserve(SchedulerNode node, SchedulerRequestKey schedulerKey, RMContainer rmContainer, Container container);

    //void setHeadroom(Resource globalLimit);

    /**

    Resource getHeadroom();

    int getNumReservedContainers(SchedulerRequestKey schedulerKey);

    void containerLaunchedOnNode(ContainerId containerId, NodeId nodeId);

    void showRequests();

    Resource getCurrentConsumption();

    void updateNMTokens(Collection<Container> containers);

    List<Container> pullPreviousAttemptContainers();

    List<Container> pullNewlyAllocatedContainers();

    void addToNewlyDemotedContainers(ContainerId containerId, RMContainer rmContainer);

    void addToNewlyDecreasedContainers(ContainerId containerId, RMContainer rmContainer);

    List<Container> pullNewlyPromotedContainers();

    List<Container> pullNewlyDemotedContainers();

    List<Container> pullNewlyIncreasedContainers();

    List<Container> pullNewlyDecreasedContainers();

    List<UpdateContainerError> pullUpdateContainerErrors();

    List<NMToken> pullUpdatedNMTokens();

    boolean isWaitingForAMContainer();

    void updateBlacklist(List<String> blacklistAdditions, List<String> blacklistRemovals);

    boolean isPlaceBlacklisted(String resourceName);

    int addMissedNonPartitionedRequestSchedulingOpportunity(SchedulerRequestKey schedulerKey);

    void resetMissedNonPartitionedRequestSchedulingOpportunity(SchedulerRequestKey schedulerKey);

    void addSchedulingOpportunity(SchedulerRequestKey schedulerKey);

    void subtractSchedulingOpportunity(SchedulerRequestKey schedulerKey);

    int getSchedulingOpportunities(SchedulerRequestKey schedulerKey);

    void resetSchedulingOpportunities(SchedulerRequestKey schedulerKey);

    void resetSchedulingOpportunities(SchedulerRequestKey schedulerKey, long currentTimeMs);

    ApplicationResourceUsageReportInterface getResourceUsageReport();

    Map<ContainerId, RMContainer> getLiveContainersMap();

    Map<SchedulerRequestKey, Long> getLastScheduledContainer();

    void transferStateFromPreviousAttempt(SchedulerApplicationAttempt appAttempt);

    void move(Queue newQueue);

    void recoverContainer(SchedulerNode node, RMContainer rmContainer);

    void incNumAllocatedContainers(NodeType containerType, NodeType requestType);

    void setApplicationHeadroomForMetrics(Resource headroom);

    void recordContainerRequestTime(long value);

    void recordContainerAllocationTime(long value);

    Set<String> getBlacklistedNodes();

    boolean hasPendingResourceRequest(String nodePartition, SchedulingMode schedulingMode);

    ResourceUsageInterface getAppAttemptResourceUsage();

    Priority getPriority();

    void setPriority(Priority appPriority);

    String getId();

    int compareInputOrderTo(SchedulableEntityInterface other);

    ResourceUsage getSchedulingResourceUsage();

    void setAppAMNodePartitionName(String partitionName);

    String getAppAMNodePartitionName();

    void updateAMContainerDiagnostics(AMState state, String diagnosticMessage);

    ReentrantReadWriteLock.WriteLock getWriteLock();

    boolean isRecovering();

    AppPlacementAllocator<N> getAppPlacementAllocator(SchedulerRequestKey schedulerRequestKey);

    void incUnconfirmedRes(Resource res);

    void decUnconfirmedRes(Resource res);

    int hashCode();

    boolean equals(Object o);

    Map<String, String> getApplicationSchedulingEnvs();

    String getPartition();

    long getStartTime();

     **/
}
