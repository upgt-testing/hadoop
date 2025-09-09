package org.apache.hadoop.yarn.server.api.records;

public interface NodeHealthStatusJVMInterface {

    void setIsNodeHealthy(boolean arg0);

    boolean getIsNodeHealthy();

    java.lang.String getHealthReport();

    long getLastHealthReportTime();

    void setHealthReport(java.lang.String arg0);

    void setLastHealthReportTime(long arg0);
}
