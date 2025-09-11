package org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt;

import org.apache.hadoop.yarn.event.AbstractEventJVMInterface;

public interface RMAppAttemptEventJVMInterface extends AbstractEventJVMInterface<org.apache.hadoop.yarn.server.resourcemanager.rmapp.attempt.RMAppAttemptEventType> {

    java.lang.String getDiagnosticMsg();

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getApplicationAttemptId();
}
