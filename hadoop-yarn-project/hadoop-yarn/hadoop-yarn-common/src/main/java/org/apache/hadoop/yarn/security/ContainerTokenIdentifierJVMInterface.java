package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface ContainerTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    java.lang.Object getProto();

    int hashCode();

    long getAllocationRequestId();

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

    java.lang.Object getExecutionType();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    int getMasterKeyId();

    java.lang.String getNodeLabelExpression();

    java.util.Set getAllcationTags();

    long getExpiryTimeStamp();
}
