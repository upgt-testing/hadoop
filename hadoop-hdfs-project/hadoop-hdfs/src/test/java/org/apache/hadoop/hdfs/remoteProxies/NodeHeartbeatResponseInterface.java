package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.server.api.records.NodeAction;
//import org.apache.hadoop.yarn.server.api.records.MasterKey;
//import org.apache.hadoop.yarn.service.api.records.Resource;
//import org.apache.hadoop.yarn.server.api.records.ContainerQueuingLimit;

public interface NodeHeartbeatResponseInterface {

    int getResponseId();

    //NodeAction getNodeAction();

    List<ContainerIdInterface> getContainersToCleanup();

    List<ContainerIdInterface> getContainersToBeRemovedFromNM();

    List<ApplicationIdInterface> getApplicationsToCleanup();

    //Map<ApplicationIdInterface, AppCollectorData> getAppCollectors();

    //void setAppCollectors(Map<ApplicationId, AppCollectorData> appCollectorsMap);

    void setResponseId(int responseId);

    //void setNodeAction(NodeAction action);

    MasterKeyInterface getContainerTokenMasterKey();

    //void setContainerTokenMasterKey(MasterKey secretKey);

    //MasterKey getNMTokenMasterKey();

    //void setNMTokenMasterKey(MasterKey secretKey);

    //void addAllContainersToCleanup(List<ContainerId> containers);

    //void addContainersToBeRemovedFromNM(List<ContainerId> containers);

    //void addAllApplicationsToCleanup(List<ApplicationId> applications);

    //List<SignalContainerRequest> getContainersToSignalList();

    //void addAllContainersToSignal(List<SignalContainerRequest> containers);

    long getNextHeartBeatInterval();

    void setNextHeartBeatInterval(long nextHeartBeatInterval);

    String getDiagnosticsMessage();

    void setDiagnosticsMessage(String diagnosticsMessage);

    boolean getAreNodeLabelsAcceptedByRM();

    void setAreNodeLabelsAcceptedByRM(boolean areNodeLabelsAcceptedByRM);

    //Resource getResource();

    //void setResource(Resource resource);

    //List<Container> getContainersToUpdate();

    //void addAllContainersToUpdate(Collection<Container> containersToUpdate);

    ContainerQueuingLimitInterface getContainerQueuingLimit();

    //void setContainerQueuingLimit(ContainerQueuingLimit containerQueuingLimit);

    //List<Container> getContainersToDecrease();

    //void addAllContainersToDecrease(Collection<Container> containersToDecrease);

    boolean getAreNodeAttributesAcceptedByRM();

    void setAreNodeAttributesAcceptedByRM(boolean areNodeAttributesAcceptedByRM);

    void setTokenSequenceNo(long tokenSequenceNo);

    long getTokenSequenceNo();

    //void setSystemCredentialsForApps(Collection<SystemCredentialsForAppsProto> systemCredentials);

    //Collection<SystemCredentialsForAppsProto> getSystemCredentialsForApps();
}
