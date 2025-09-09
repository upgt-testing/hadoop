package org.apache.hadoop.yarn.server.nodemanager.containermanager;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface AuxServicesEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.nodemanager.containermanager.AuxServicesEventType> {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationID();

    java.lang.String getUser();

    java.lang.String getServiceID();

    java.nio.ByteBuffer getServiceData();

    java.lang.Object getContainer();
}
