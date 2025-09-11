package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.yarn.state.MultiStateTransitionListenerJVMInterface;

public interface DefaultContainerStateListenerJVMInterface extends MultiStateTransitionListenerJVMInterface<org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerImpl, org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerEvent, org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerState>, ContainerStateTransitionListenerJVMInterface {

    void init_bridge(java.lang.Object arg0);
}
