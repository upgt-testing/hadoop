package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface ApplicationEventDispatcherJVMInterface extends EventHandlerJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppEvent> {

    void handle_bridge(org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppEventJVMInterface arg0);
}
