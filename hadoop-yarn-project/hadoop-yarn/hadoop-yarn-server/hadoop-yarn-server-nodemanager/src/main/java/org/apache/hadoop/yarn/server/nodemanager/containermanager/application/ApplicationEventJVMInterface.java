package org.apache.hadoop.yarn.server.nodemanager.containermanager.application;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface ApplicationEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationEventType> {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationID();
}
