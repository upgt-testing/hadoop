package org.apache.hadoop.security;

import org.apache.hadoop.io.WritableJVMInterface;

public interface CredentialsJVMInterface extends WritableJVMInterface {

    void addSecretKey_bridge(org.apache.hadoop.io.TextJVMInterface arg0, byte[] arg1);

    void readTokenStorageStream(java.io.DataInputStream arg0) throws java.io.IOException;

    void addToken_bridge(org.apache.hadoop.io.TextJVMInterface arg0, org.apache.hadoop.security.token.TokenJVMInterface arg1);

    void removeSecretKey_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    void writeTokenStorageFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.conf.ConfigurationJVMInterface arg1) throws java.io.IOException;

    java.util.Collection getAllTokens();

    java.util.Map getSecretKeyMap();

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.security.token.TokenJVMInterface getToken_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    void writeTokenStorageToStream(java.io.DataOutputStream arg0) throws java.io.IOException;

    java.util.Map getTokenMap();

    int numberOfSecretKeys();

    void writeTokenStorageFile_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.conf.ConfigurationJVMInterface arg1, java.lang.Object arg2) throws java.io.IOException;

    int numberOfTokens();

    void mergeAll_bridge(org.apache.hadoop.security.CredentialsJVMInterface arg0);

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    byte[] getSecretKey_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    void addAll_bridge(org.apache.hadoop.security.CredentialsJVMInterface arg0);

    java.util.List getAllSecretKeys();

    void writeTokenStorageToStream_bridge(java.io.DataOutputStream arg0, java.lang.Object arg1) throws java.io.IOException;
}
