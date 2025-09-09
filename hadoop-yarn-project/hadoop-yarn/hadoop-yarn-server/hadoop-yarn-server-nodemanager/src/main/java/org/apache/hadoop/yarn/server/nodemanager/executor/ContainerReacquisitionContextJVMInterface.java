package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerReacquisitionContextJVMInterface {

    java.lang.String getUser();

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerId();

    java.lang.Object getContainer();
}
