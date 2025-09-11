package org.apache.hadoop.yarn.server.federation.store.records;

public interface SubClusterPolicyConfigurationJVMInterface {

    int hashCode();

    boolean equals(java.lang.Object arg0);

    void setParams(java.nio.ByteBuffer arg0);

    java.lang.String toString();

    java.nio.ByteBuffer getParams();

    void setQueue(java.lang.String arg0);

    java.lang.String getType();

    void setType(java.lang.String arg0);

    java.lang.String getQueue();
}
