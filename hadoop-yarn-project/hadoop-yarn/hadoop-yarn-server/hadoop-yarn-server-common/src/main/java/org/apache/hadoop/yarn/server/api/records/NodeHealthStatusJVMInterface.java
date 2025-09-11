package org.apache.hadoop.yarn.server.api.records;

public interface NodeHealthStatusJVMInterface {

    void setIsNodeHealthy(boolean arg0);

    boolean getIsNodeHealthy();

    long getLastHealthReportTime();

    java.lang.String getHealthReport();

    void setHealthReport(java.lang.String arg0);

    void setLastHealthReportTime(long arg0);
}
