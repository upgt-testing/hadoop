package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface ContainerTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    long getAllocationRequestId();

    int hashCode();

    java.lang.Object getProto();

    java.lang.Object getContainerType();

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerID();

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    java.lang.String toString();

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
