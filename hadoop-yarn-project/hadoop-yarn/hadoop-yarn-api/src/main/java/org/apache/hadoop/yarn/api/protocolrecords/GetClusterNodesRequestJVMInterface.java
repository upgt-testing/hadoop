package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetClusterNodesRequestJVMInterface {

    java.util.EnumSet getNodeStates();

    void setNodeStates(java.util.EnumSet<org.apache.hadoop.yarn.api.records.NodeState> arg0);
}
