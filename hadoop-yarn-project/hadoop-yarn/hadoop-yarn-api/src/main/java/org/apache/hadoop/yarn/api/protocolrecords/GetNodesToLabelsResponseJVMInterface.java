package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetNodesToLabelsResponseJVMInterface {

    void setNodeToLabels(java.util.Map<org.apache.hadoop.yarn.api.records.NodeId, java.util.Set<java.lang.String>> arg0);

    java.util.Map getNodeToLabels();
}
