package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.service.CompositeServiceJVMInterface;
import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface NodeManagerJVMInterface extends CompositeServiceJVMInterface, EventHandlerJVMInterface<org.apache.hadoop.yarn.server.nodemanager.NodeManagerEvent>, NodeManagerMXBeanJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.health.NodeHealthCheckerServiceJVMInterface getNodeHealthChecker();

    java.lang.Object getNMContext();

    java.lang.Object getNodeStatusUpdater();

    boolean isSecurityEnabled();

    java.lang.String getName();

    void handle_bridge(org.apache.hadoop.yarn.server.nodemanager.NodeManagerEventJVMInterface arg0);
}
