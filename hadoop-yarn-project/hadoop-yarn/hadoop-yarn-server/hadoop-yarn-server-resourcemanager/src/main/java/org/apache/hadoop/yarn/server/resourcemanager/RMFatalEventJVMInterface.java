package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface RMFatalEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.RMFatalEventType> {

    java.lang.String toString();

    java.lang.String getExplanation();
}
