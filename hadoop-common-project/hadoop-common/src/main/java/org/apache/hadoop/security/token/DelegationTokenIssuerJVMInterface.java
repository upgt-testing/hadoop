package org.apache.hadoop.security.token;

public interface DelegationTokenIssuerJVMInterface {

    java.lang.String getCanonicalServiceName();

    org.apache.hadoop.security.token.TokenJVMInterface[] addDelegationTokens_bridge(java.lang.String arg0, org.apache.hadoop.security.CredentialsJVMInterface arg1) throws java.io.IOException;

    java.lang.Object[] getAdditionalTokenIssuers() throws java.io.IOException;

    org.apache.hadoop.security.token.TokenJVMInterface getDelegationToken(java.lang.String arg0) throws java.io.IOException;
}
