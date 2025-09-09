package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetContainerReportResponseJVMInterface {

    org.apache.hadoop.yarn.api.records.ContainerReportJVMInterface getContainerReport();

    void setContainerReport_bridge(org.apache.hadoop.yarn.api.records.ContainerReportJVMInterface arg0);
}
