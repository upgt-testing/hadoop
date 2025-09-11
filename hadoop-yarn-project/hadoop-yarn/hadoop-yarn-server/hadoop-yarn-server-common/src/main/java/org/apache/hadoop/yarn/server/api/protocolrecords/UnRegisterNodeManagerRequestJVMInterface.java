package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface UnRegisterNodeManagerRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);
}
