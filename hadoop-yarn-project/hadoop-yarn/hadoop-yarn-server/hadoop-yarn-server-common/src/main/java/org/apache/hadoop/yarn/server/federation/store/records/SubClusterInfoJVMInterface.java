package org.apache.hadoop.yarn.server.federation.store.records;

public interface SubClusterInfoJVMInterface {

    int hashCode();

    void setState_bridge(java.lang.Object arg0);

    java.lang.Object getState();

    java.lang.String getCapability();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.lang.String getRMAdminServiceAddress();

    void setRMWebServiceAddress(java.lang.String arg0);

    void setAMRMServiceAddress(java.lang.String arg0);

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface getSubClusterId();

    void setRMAdminServiceAddress(java.lang.String arg0);

    void setLastHeartBeat(long arg0);

    long getLastHeartBeat();

    java.lang.String getAMRMServiceAddress();

    long getLastStartTime();

    java.lang.String getRMWebServiceAddress();

    java.lang.String getClientRMServiceAddress();

    void setLastStartTime(long arg0);

    void setSubClusterId_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface arg0);

    void setClientRMServiceAddress(java.lang.String arg0);

    void setCapability(java.lang.String arg0);
}
