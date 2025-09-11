package org.apache.hadoop.yarn.server.nodemanager.security;

import org.apache.hadoop.yarn.server.security.BaseNMTokenSecretManagerJVMInterface;

public interface NMTokenSecretManagerInNMJVMInterface extends BaseNMTokenSecretManagerJVMInterface {

    void recover() throws java.io.IOException;

    void appAttemptStartContainer_bridge(org.apache.hadoop.yarn.security.NMTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;

    byte[] retrievePassword_bridge(org.apache.hadoop.yarn.security.NMTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;

    boolean isAppAttemptNMTokenKeyPresent_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.NodeIdJVMInterface getNodeId();

    void setMasterKey_bridge(java.lang.Object arg0);

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    void appFinished_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);
}
