package org.apache.hadoop.yarn.server.federation.store.records;

public interface SubClusterInfoJVMInterface {

    void setState_bridge(java.lang.Object arg0);

    int hashCode();

    java.lang.Object getState();

    boolean equals(java.lang.Object arg0);

    java.lang.String getCapability();

    java.lang.String toString();

    java.lang.String getRMAdminServiceAddress();

    void setAMRMServiceAddress(java.lang.String arg0);

    void setRMWebServiceAddress(java.lang.String arg0);

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface getSubClusterId();

    void setRMAdminServiceAddress(java.lang.String arg0);

    void setLastHeartBeat(long arg0);

    long getLastHeartBeat();

    java.lang.String getAMRMServiceAddress();

    long getLastStartTime();

    java.lang.String getClientRMServiceAddress();

    java.lang.String getRMWebServiceAddress();

    void setLastStartTime(long arg0);

    void setSubClusterId_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface arg0);

    void setClientRMServiceAddress(java.lang.String arg0);

    void setCapability(java.lang.String arg0);
}
