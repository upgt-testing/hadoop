package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface UnRegisterNodeManagerRequestJVMInterface {

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();
}
