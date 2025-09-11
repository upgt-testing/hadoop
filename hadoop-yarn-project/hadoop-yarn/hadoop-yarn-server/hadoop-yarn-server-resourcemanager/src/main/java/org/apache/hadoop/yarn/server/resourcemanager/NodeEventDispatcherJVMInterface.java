package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface NodeEventDispatcherJVMInterface extends EventHandlerJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.rmnode.RMNodeEvent> {

    void handle_bridge(org.apache.hadoop.yarn.server.resourcemanager.rmnode.RMNodeEventJVMInterface arg0);
}
