package org.apache.hadoop.yarn.server.api;

public interface ResourceTrackerJVMInterface {

    org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatResponseJVMInterface nodeHeartbeat_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.server.api.protocolrecords.UnRegisterNodeManagerResponseJVMInterface unRegisterNodeManager_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.UnRegisterNodeManagerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.server.api.protocolrecords.RegisterNodeManagerResponseJVMInterface registerNodeManager_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.RegisterNodeManagerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}
