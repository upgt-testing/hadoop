package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetClusterMetricsResponseJVMInterface {

    void setClusterMetrics_bridge(org.apache.hadoop.yarn.api.records.YarnClusterMetricsJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.YarnClusterMetricsJVMInterface getClusterMetrics();
}
