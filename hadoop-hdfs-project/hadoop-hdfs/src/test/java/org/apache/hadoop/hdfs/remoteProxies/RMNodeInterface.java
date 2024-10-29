package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.net.Node;

public interface RMNodeInterface {

    NodeIdInterface getNodeID();

    String getHostName();

    int getCommandPort();

    int getHttpPort();

    String getNodeAddress();

    String getHttpAddress();

    String getHealthReport();

    long getLastHealthReportTime();

    String getNodeManagerVersion();

    ResourceInterface getTotalCapability();

    ResourceUtilizationInterface getAggregatedContainersUtilization();

    ResourceUtilizationInterface getNodeUtilization();

    String getRackName();

    Node getNode();

    //NodeState getState();

    List<ContainerIdInterface> getContainersToCleanUp();

    List<ApplicationIdInterface> getAppsToCleanup();

    NodeHeartbeatResponseInterface getLastNodeHeartBeatResponse();

    //List<UpdatedContainerInfo> pullContainerUpdates();

    Set<String> getNodeLabels();

    List<ContainerInterface> pullNewlyIncreasedContainers();
}
