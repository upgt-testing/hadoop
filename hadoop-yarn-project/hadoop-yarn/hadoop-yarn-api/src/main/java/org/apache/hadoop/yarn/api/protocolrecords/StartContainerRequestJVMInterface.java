package org.apache.hadoop.yarn.api.protocolrecords;

public interface StartContainerRequestJVMInterface {

    void setContainerToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getContainerToken();

    void setContainerLaunchContext_bridge(org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ContainerLaunchContextJVMInterface getContainerLaunchContext();
}
