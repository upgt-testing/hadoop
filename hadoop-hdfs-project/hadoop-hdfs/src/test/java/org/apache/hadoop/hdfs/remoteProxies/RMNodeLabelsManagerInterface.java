package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.api.records.NodeId;
//import org.apache.hadoop.yarn.service.api.records.Resource;
import org.apache.hadoop.security.UserGroupInformation;
//import org.apache.hadoop.yarn.server.resourcemanager.RMContext;

public interface RMNodeLabelsManagerInterface {

    /**
    void addLabelsToNode(Map<NodeId, Set<String>> addedLabelsToNode);

    void removeFromClusterNodeLabels(Collection<String> labelsToRemove);

    void addToCluserNodeLabels(Collection<NodeLabel> labels);

    void removeLabelsFromNode(Map<NodeId, Set<String>> removeLabelsFromNode);

    void replaceLabelsOnNode(Map<NodeId, Set<String>> replaceLabelsToNode);

    void activateNode(NodeId nodeId, Resource resource);

    void deactivateNode(NodeId nodeId);

    void updateNodeResource(NodeId node, Resource newResource);

    void reinitializeQueueLabels(Map<String, Set<String>> queueToLabels);

    Resource getQueueResource(String queueName, Set<String> queueLabels, Resource clusterResource);

    int getActiveNMCountPerLabel(String label);

    Set<String> getLabelsOnNode(NodeId nodeId);

    boolean containsNodeLabel(String label);

    Resource getResourceByLabel(String label, Resource clusterResource);

    boolean checkAccess(UserGroupInformation user);

    void setRMContext(RMContext rmContext);

    List<RMNodeLabel> pullRMNodeLabelsInfo();
     **/
}
