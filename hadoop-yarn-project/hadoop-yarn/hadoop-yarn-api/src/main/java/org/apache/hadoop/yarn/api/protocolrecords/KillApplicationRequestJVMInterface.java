package org.apache.hadoop.yarn.api.protocolrecords;

public interface KillApplicationRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    void setDiagnostics(java.lang.String arg0);

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    java.lang.String getDiagnostics();
}
