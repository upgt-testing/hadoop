package org.apache.hadoop.hdfs.remoteProxies;

public interface ErasureCodingPolicyInfoInterface {
    boolean equals(java.lang.Object arg0);
    java.lang.String toString();
    int hashCode();
    boolean isEnabled();
    boolean isDisabled();
    ErasureCodingPolicyInterface getPolicy();
    org.apache.hadoop.hdfs.protocol.ErasureCodingPolicyState getState();
    boolean isRemoved();
    void setState(org.apache.hadoop.hdfs.protocol.ErasureCodingPolicyState arg0);
}