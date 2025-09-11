package org.apache.hadoop.yarn.server.nodemanager.containermanager.container;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface ContainerEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerEventType> {

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerID();
}
