package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface ApplicationAttemptEventDispatcherJVMInterface extends EventHandlerJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptEvent> {

    void handle_bridge(org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptEventJVMInterface arg0);
}
