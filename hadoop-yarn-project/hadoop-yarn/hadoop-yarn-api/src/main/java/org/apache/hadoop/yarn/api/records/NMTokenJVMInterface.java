package org.apache.hadoop.yarn.api.records;

public interface NMTokenJVMInterface {

    int hashCode();

    boolean equals(java.lang.Object arg0);

    void setToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getToken();

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();
}
