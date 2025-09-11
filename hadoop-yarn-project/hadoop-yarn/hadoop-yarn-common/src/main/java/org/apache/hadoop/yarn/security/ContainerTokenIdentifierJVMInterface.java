package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface ContainerTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    long getAllocationRequestId();

    int hashCode();

    java.lang.Object getProto();

    java.lang.Object getContainerType();

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    java.lang.String toString();

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerID();

    int getVersion();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    long getCreationTime();

    java.lang.String getApplicationSubmitter();

    java.lang.String getNmHostAddress();

    org.apache.hadoop.yarn.api.records.LogAggregationContextJVMInterface getLogAggregationContext();

    long getRMIdentifier();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    java.lang.Object getExecutionType();

    java.lang.String getNodeLabelExpression();

    int getMasterKeyId();

    java.util.Set getAllcationTags();

    long getExpiryTimeStamp();
}
