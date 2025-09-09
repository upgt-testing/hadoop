package org.apache.hadoop.yarn.server.security;

import org.apache.hadoop.security.token.SecretManagerJVMInterface;

public interface BaseNMTokenSecretManagerJVMInterface extends SecretManagerJVMInterface<org.apache.hadoop.yarn.security.NMTokenIdentifier> {

    org.apache.hadoop.yarn.api.records.TokenJVMInterface createNMToken_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0, org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg1, java.lang.String arg2);

    //org.apache.hadoop.yarn.security.NMTokenIdentifierJVMInterface createIdentifier();

    byte[] retrievePassword_bridge(org.apache.hadoop.yarn.security.NMTokenIdentifierJVMInterface arg0) throws org.apache.hadoop.security.token.SecretManager.InvalidToken;

    java.lang.Object getCurrentKey();
}
