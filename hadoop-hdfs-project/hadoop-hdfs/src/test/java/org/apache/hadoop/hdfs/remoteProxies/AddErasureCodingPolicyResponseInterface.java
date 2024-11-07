package org.apache.hadoop.hdfs.remoteProxies;

public interface AddErasureCodingPolicyResponseInterface {
    ErasureCodingPolicyInterface getPolicy();
    java.lang.String getErrorMsg();
    int hashCode();
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    boolean isSucceed();
}