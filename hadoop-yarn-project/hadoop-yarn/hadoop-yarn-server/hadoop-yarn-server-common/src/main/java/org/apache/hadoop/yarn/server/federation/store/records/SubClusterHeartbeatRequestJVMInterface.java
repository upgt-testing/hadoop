package org.apache.hadoop.yarn.server.federation.store.records;

public interface SubClusterHeartbeatRequestJVMInterface {

    void setState_bridge(java.lang.Object arg0);

    void setLastHeartBeat(long arg0);

    long getLastHeartBeat();

    java.lang.Object getState();

    java.lang.String getCapability();

    java.lang.String toString();

    void setSubClusterId_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface arg0);

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface getSubClusterId();

    void setCapability(java.lang.String arg0);
}
