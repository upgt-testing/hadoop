package org.apache.hadoop.yarn.server.nodemanager.health;

public interface HealthReporterJVMInterface {

    boolean isHealthy();

    long getLastHealthReportTime();

    java.lang.String getHealthReport();
}
