package org.apache.hadoop.yarn.server.nodemanager.health;

import org.apache.hadoop.service.CompositeServiceJVMInterface;

public interface NodeHealthCheckerServiceJVMInterface extends CompositeServiceJVMInterface, HealthReporterJVMInterface {

    boolean isHealthy();

    void reportException(java.lang.Exception arg0);

    java.lang.String getHealthReport();

    long getLastHealthReportTime();

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getDiskHandler();
}
