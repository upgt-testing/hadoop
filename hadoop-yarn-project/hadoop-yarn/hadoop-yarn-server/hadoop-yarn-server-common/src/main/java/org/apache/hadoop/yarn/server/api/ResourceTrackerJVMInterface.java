package org.apache.hadoop.yarn.server.api;

public interface ResourceTrackerJVMInterface {

    java.lang.Object nodeHeartbeat_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    org.apache.hadoop.yarn.server.api.protocolrecords.UnRegisterNodeManagerResponseJVMInterface unRegisterNodeManager_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.UnRegisterNodeManagerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;

    java.lang.Object registerNodeManager_bridge(org.apache.hadoop.yarn.server.api.protocolrecords.RegisterNodeManagerRequestJVMInterface arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.io.IOException;
}
