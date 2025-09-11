package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetContainerReportRequestJVMInterface {

    void setContainerId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerId();
}
