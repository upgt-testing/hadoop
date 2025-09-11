package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetClusterNodeLabelsResponseJVMInterface {

    java.util.Set getNodeLabels();

    void setNodeLabelList(java.util.List<org.apache.hadoop.yarn.api.records.NodeLabel> arg0);

    java.util.List getNodeLabelList();

    void setNodeLabels(java.util.Set<java.lang.String> arg0);
}
