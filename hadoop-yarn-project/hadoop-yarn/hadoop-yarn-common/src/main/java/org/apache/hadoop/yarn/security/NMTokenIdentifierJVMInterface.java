package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface NMTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    int hashCode();

    java.lang.Object getProto();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    int getKeyId();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    java.lang.String getApplicationSubmitter();

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getApplicationAttemptId();
}
