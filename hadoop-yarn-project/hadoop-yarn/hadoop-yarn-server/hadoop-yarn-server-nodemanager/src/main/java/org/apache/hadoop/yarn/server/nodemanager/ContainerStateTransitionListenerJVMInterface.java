package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.yarn.state.StateTransitionListenerJVMInterface;

public interface ContainerStateTransitionListenerJVMInterface extends StateTransitionListenerJVMInterface<org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerImpl, org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerEvent, org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerState> {

    void init_bridge(java.lang.Object arg0);
}
