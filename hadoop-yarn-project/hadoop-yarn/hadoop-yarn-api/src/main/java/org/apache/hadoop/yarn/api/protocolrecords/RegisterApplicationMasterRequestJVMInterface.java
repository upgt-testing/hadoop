package org.apache.hadoop.yarn.api.protocolrecords;

public interface RegisterApplicationMasterRequestJVMInterface {

    java.util.Map getPlacementConstraints();

    void setRpcPort(int arg0);

    void setPlacementConstraints(java.util.Map<java.util.Set<java.lang.String>, org.apache.hadoop.yarn.api.resource.PlacementConstraint> arg0);

    int getRpcPort();

    java.lang.String getTrackingUrl();

    void setHost(java.lang.String arg0);

    void setTrackingUrl(java.lang.String arg0);

    java.lang.String getHost();
}
