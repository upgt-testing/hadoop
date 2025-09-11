package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface ContainerTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    java.lang.Object getProto();

    int hashCode();

    java.lang.Object getContainerType();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getResource();

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface getContainerID();

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    java.lang.String toString();

    int getVersion();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    long getCreationTime();

    java.lang.String getNmHostAddress();

    java.lang.String getApplicationSubmitter();

    org.apache.hadoop.yarn.api.records.LogAggregationContextJVMInterface getLogAggregationContext();

    long getRMIdentifier();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.Object getExecutionType();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    int getMasterKeyId();

    java.lang.String getNodeLabelExpression();

    long getExpiryTimeStamp();
}
