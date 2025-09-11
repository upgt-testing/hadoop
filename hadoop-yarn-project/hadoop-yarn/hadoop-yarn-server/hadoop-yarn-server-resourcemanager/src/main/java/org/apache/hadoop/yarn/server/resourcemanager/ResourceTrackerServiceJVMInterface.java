package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;
import org.apache.hadoop.yarn.server.api.ResourceTrackerJVMInterface;

public interface ResourceTrackerServiceJVMInterface extends AbstractServiceJVMInterface, ResourceTrackerJVMInterface {

    void loadDynamicResourceConfiguration_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException;

    org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatResponseJVMInterface nodeHeartbeat_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.ipc.ServerJVMInterface getServer();

    org.apache.hadoop.yarn.server.api.protocolrecords.UnRegisterNodeManagerResponseJVMInterface unRegisterNodeManager_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.UnRegisterNodeManagerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    void updateDynamicResourceConfiguration_bridge(org.apache.hadoop.yarn.server.resourcemanager.resource.DynamicResourceConfigurationJVMInterface arg0);

    org.apache.hadoop.yarn.server.api.protocolrecords.RegisterNodeManagerResponseJVMInterface registerNodeManager_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.RegisterNodeManagerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}
