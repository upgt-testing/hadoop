package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
/**
import org.apache.hadoop.yarn.service.api.records.Resource;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.server.resourcemanager.rmcontainer.RMContainer;
import org.apache.hadoop.yarn.api.records.ResourceUtilization;
 **/

public interface SchedulerNodeInterface {

    RMNodeInterface getRMNode();

    //void updateTotalResource(Resource resource);

    void setOvercommitTimeOut(long timeOut);

    boolean isOvercommitTimedOut();

    boolean isOvercommitTimeOutSet();

    NodeIdInterface getNodeID();

    String getHttpAddress();

    String getNodeName();

    String getRackName();

    void allocateContainer(RMContainerInterface rmContainer);

    ResourceInterface getUnallocatedResource();

    ResourceInterface getAllocatedResource();

    ResourceInterface getTotalResource();

    //boolean isValidContainer(ContainerId containerId);

    //void releaseContainer(ContainerId containerId, boolean releasedByNode);

    //void containerStarted(ContainerId containerId);

    //void deductUnallocatedResource(Resource resource);

    //void reserveResource(SchedulerApplicationAttempt attempt, SchedulerRequestKeyInterface schedulerKey, RMContainer container);

    //void unreserveResource(SchedulerApplicationAttempt attempt);

    String toString();

    int getNumContainers();

    List<RMContainerInterface> getCopiedListOfRunningContainers();

    List<RMContainerInterface> getRunningContainersWithAMsAtTheEnd();

    List<RMContainerInterface> getContainersToKill();

    RMContainerInterface getReservedContainer();

    //void setReservedContainer(RMContainer reservedContainer);

    //void recoverContainer(RMContainer rmContainer);

    Set<String> getLabels();

    void updateLabels(Set<String> labels);

    String getPartition();

    //void setAggregatedContainersUtilization(ResourceUtilization containersUtilization);

    ResourceUtilizationInterface getAggregatedContainersUtilization();

    //void setNodeUtilization(ResourceUtilization nodeUtilization);

    ResourceUtilizationInterface getNodeUtilization();

    long getLastHeartbeatMonotonicTime();

    void notifyNodeUpdate();

    boolean equals(Object o);

    int hashCode();

    //Set<NodeAttribute> getNodeAttributes();

    //void updateNodeAttributes(Set<NodeAttribute> attributes);
}
