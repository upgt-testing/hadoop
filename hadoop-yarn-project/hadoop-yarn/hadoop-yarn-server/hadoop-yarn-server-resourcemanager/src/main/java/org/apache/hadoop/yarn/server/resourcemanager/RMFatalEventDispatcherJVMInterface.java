package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface RMFatalEventDispatcherJVMInterface extends EventHandlerJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.RMFatalEvent> {

    void handle_bridge(org.apache.hadoop.yarn.server.resourcemanager.RMFatalEventJVMInterface arg0);
}
