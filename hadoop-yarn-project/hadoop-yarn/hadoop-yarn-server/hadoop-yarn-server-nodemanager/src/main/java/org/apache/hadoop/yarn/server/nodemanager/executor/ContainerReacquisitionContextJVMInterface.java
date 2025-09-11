package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerReacquisitionContextJVMInterface {

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerId();

    java.lang.String getUser();

    java.lang.Object getContainer();
}
