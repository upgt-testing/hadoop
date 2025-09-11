package org.apache.hadoop.yarn.api.protocolrecords;

public interface SignalContainerRequestJVMInterface {

    void setContainerId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void setCommand_bridge(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerId();

    java.lang.Object getCommand();
}
