package org.apache.hadoop.yarn.server.federation.store.records;

public interface ApplicationHomeSubClusterJVMInterface {

    int hashCode();

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface getHomeSubCluster();

    void setApplicationId_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    void setHomeSubCluster_bridge(org.apache.hadoop.yarn.server.federation.store.records.SubClusterIdJVMInterface arg0);
}
