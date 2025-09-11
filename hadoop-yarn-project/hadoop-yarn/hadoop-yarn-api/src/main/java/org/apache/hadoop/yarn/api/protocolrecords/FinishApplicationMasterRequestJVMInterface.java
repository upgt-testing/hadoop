package org.apache.hadoop.yarn.api.protocolrecords;

public interface FinishApplicationMasterRequestJVMInterface {

    void setDiagnostics(java.lang.String arg0);

    java.lang.String getTrackingUrl();

    void setFinalApplicationStatus_bridge(java.lang.Object arg0);

    java.lang.String getDiagnostics();

    java.lang.Object getFinalApplicationStatus();

    void setTrackingUrl(java.lang.String arg0);
}
