package org.apache.hadoop.yarn.server.resourcemanager.rmapp;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface RMAppEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMAppEventType> {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    java.lang.String getDiagnosticMsg();
}
