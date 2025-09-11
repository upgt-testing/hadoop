package org.apache.hadoop.yarn.server.nodemanager.security;

import org.apache.hadoop.yarn.server.security.BaseContainerTokenSecretManagerJVMInterface;

public interface NMContainerTokenSecretManagerJVMInterface extends BaseContainerTokenSecretManagerJVMInterface {

    void recover() throws java.io.IOException;

    void startContainerSuccessful_bridge(org.apache.hadoop.yarn.security.ContainerTokenIdentifierJVMInterface arg0);

    boolean isValidStartContainerRequest_bridge(org.apache.hadoop.yarn.security.ContainerTokenIdentifierJVMInterface arg0);

    void setNodeId_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    void setMasterKey_bridge(java.lang.Object arg0);

    byte[] retrievePassword_bridge(org.apache.hadoop.yarn.security.ContainerTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;
}
