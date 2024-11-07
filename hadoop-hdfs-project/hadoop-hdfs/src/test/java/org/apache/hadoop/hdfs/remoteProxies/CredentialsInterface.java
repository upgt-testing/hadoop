package org.apache.hadoop.hdfs.remoteProxies;

public interface CredentialsInterface {
    void addAll(CredentialsInterface arg0);
    void writeTokenStorageToStream(java.io.DataOutputStream arg0) throws java.io.IOException;
    void readProto(java.io.DataInput arg0) throws java.io.IOException;
    void writeProtobufOutputStream(java.io.DataOutputStream arg0) throws java.io.IOException;
    CredentialsInterface readTokenStorageFile(PathInterface arg0, ConfigurationInterface arg1) throws java.io.IOException;
    void writeTokenStorageFile(PathInterface arg0, ConfigurationInterface arg1, org.apache.hadoop.security.Credentials.SerializedFormat arg2) throws java.io.IOException;
    void readTokenStorageStream(java.io.DataInputStream arg0) throws java.io.IOException;
    void addAll(CredentialsInterface arg0, boolean arg1);
    void writeTokenStorageToStream(java.io.DataOutputStream arg0, org.apache.hadoop.security.Credentials.SerializedFormat arg1) throws java.io.IOException;
    int numberOfSecretKeys();
    void removeSecretKey(TextInterface arg0);
    void addSecretKey(TextInterface arg0, byte[] arg1);
    java.util.Collection<org.apache.hadoop.security.token.Token<? extends org.apache.hadoop.security.token.TokenIdentifier>> getAllTokens();
//    void addToken(TextInterface arg0, TokenInterface<? extends TokenInterfaceIdentifier> arg1);
    void mergeAll(CredentialsInterface arg0);
    void writeTokenStorageFile(PathInterface arg0, ConfigurationInterface arg1) throws java.io.IOException;
    java.util.Map<org.apache.hadoop.io.Text, byte[]> getSecretKeyMap();
    byte[] getSecretKey(TextInterface arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void writeWritableOutputStream(java.io.DataOutputStream arg0) throws java.io.IOException;
    CredentialsInterface readTokenStorageFile(java.io.File arg0, ConfigurationInterface arg1) throws java.io.IOException;
    java.util.List<org.apache.hadoop.io.Text> getAllSecretKeys();
    TokenInterface<? extends org.apache.hadoop.security.token.TokenIdentifier> getToken(TextInterface arg0);
    int numberOfTokens();
    void writeProto(java.io.DataOutput arg0) throws java.io.IOException;
    java.util.Map<org.apache.hadoop.io.Text, org.apache.hadoop.security.token.Token<? extends org.apache.hadoop.security.token.TokenIdentifier>> getTokenMap();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
}