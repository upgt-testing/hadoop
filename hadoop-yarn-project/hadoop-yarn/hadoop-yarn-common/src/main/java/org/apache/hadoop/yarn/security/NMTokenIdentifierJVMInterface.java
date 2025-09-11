package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface NMTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    int hashCode();

    java.lang.Object getProto();

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    java.lang.String toString();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    int getKeyId();

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getApplicationAttemptId();

    java.lang.String getApplicationSubmitter();
}
