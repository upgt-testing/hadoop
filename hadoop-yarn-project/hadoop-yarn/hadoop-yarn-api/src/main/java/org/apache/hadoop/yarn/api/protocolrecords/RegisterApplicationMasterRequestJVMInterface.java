package org.apache.hadoop.yarn.api.protocolrecords;

public interface RegisterApplicationMasterRequestJVMInterface {

    void setRpcPort(int arg0);

    int getRpcPort();

    java.lang.String getTrackingUrl();

    void setHost(java.lang.String arg0);

    java.lang.String getHost();

    void setTrackingUrl(java.lang.String arg0);
}
