package org.apache.hadoop.yarn.server.nodemanager.containermanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;
import org.apache.hadoop.service.ServiceStateChangeListenerJVMInterface;
import org.apache.hadoop.yarn.event.EventHandlerJVMInterface;

public interface AuxServicesJVMInterface extends AbstractServiceJVMInterface, ServiceStateChangeListenerJVMInterface, EventHandlerJVMInterface<org.apache.hadoop.yarn.server.nodemanager.containermanager.AuxServicesEvent> {

    java.util.Map getMetaData();

    void serviceStart() throws java.lang.Exception;

    void serviceInit_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.lang.Exception;

    void serviceStop() throws java.lang.Exception;

    void stateChanged_bridge(java.lang.Object arg0);

    java.util.Collection getServiceRecords();

    void handle_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.AuxServicesEventJVMInterface arg0);

    boolean isManifestEnabled();

    void reload_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordsJVMInterface arg0) throws java.io.IOException;
}
