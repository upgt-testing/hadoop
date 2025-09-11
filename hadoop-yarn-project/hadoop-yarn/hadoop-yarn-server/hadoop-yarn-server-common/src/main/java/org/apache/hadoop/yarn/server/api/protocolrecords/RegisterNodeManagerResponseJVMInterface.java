package org.apache.hadoop.yarn.server.api.protocolrecords;

public interface RegisterNodeManagerResponseJVMInterface {

    void setResource(org.apache.hadoop.yarn.api.records.Resource arg0);

    boolean getAreNodeLabelsAcceptedByRM();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    java.lang.Object getContainerTokenMasterKey();


    java.lang.String getRMVersion();

    java.lang.Object getNodeAction();

    void setRMIdentifier(long arg0);

    java.lang.Object getNMTokenMasterKey();

    long getRMIdentifier();

    void setDiagnosticsMessage(java.lang.String arg0);

    java.lang.String getDiagnosticsMessage();

    void setRMVersion(java.lang.String arg0);

    void setAreNodeLabelsAcceptedByRM(boolean arg0);
}
