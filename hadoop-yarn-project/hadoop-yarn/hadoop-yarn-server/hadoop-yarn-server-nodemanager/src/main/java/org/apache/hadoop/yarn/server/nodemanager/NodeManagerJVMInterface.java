package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.service.CompositeServiceJVMInterface;
import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface NodeManagerJVMInterface extends CompositeServiceJVMInterface, EventHandlerJVMInterface<org.apache.hadoop.yarn.server.nodemanager.NodeManagerEvent>, NodeManagerMXBeanJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.NodeHealthCheckerServiceJVMInterface getNodeHealthChecker();

    java.lang.Object getNMContext();

    boolean isSecurityEnabled();

    java.lang.Object getNodeStatusUpdater();

    java.lang.String getName();

    void handle_bridge(org.apache.hadoop.yarn.server.nodemanager.NodeManagerEventJVMInterface arg0);
}
