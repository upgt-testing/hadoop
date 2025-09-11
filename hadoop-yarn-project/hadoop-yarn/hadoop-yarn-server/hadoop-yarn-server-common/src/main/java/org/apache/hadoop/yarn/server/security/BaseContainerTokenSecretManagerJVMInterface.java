package org.apache.hadoop.yarn.server.security;

import org.apache.hadoop.security.token.SecretManagerJVMInterface;

public interface BaseContainerTokenSecretManagerJVMInterface extends SecretManagerJVMInterface<org.apache.hadoop.yarn.security.ContainerTokenIdentifier> {

    byte[] createPassword_bridge(org.apache.hadoop.yarn.security.ContainerTokenIdentifierJVMInterface arg0);

    byte[] retrievePassword_bridge(org.apache.hadoop.yarn.security.ContainerTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;

    java.lang.Object getCurrentKey();
}
