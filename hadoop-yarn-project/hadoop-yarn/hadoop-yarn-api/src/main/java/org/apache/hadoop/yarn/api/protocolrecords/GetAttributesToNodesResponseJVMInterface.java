package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetAttributesToNodesResponseJVMInterface {

    void setAttributeToNodes(java.util.Map<org.apache.hadoop.yarn.api.records.NodeAttributeKey, java.util.List<org.apache.hadoop.yarn.api.records.NodeToAttributeValue>> arg0);

    java.util.Map getAttributesToNodes();
}
