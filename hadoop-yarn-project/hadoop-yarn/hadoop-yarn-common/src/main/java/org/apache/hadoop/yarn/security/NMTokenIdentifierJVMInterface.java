package org.apache.hadoop.yarn.security;

import org.apache.hadoop.security.token.TokenIdentifierJVMInterface;

public interface NMTokenIdentifierJVMInterface extends TokenIdentifierJVMInterface {

    java.lang.Object getProto();

    int hashCode();

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.security.UserGroupInformationJVMInterface getUser();

    java.lang.String toString();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    int getKeyId();

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getApplicationAttemptId();

    java.lang.String getApplicationSubmitter();
}
