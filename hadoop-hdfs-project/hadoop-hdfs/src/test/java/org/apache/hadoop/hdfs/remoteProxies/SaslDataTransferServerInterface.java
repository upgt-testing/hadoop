package org.apache.hadoop.hdfs.remoteProxies;

public interface SaslDataTransferServerInterface {
    void sendInvalidTokenSaslErrorMessage(java.io.DataOutputStream arg0, java.lang.String arg1) throws java.io.IOException;
    java.lang.String getNegotiatedQOP();
    IOStreamPairInterface receive(org.apache.hadoop.hdfs.net.Peer arg0, java.io.OutputStream arg1, java.io.InputStream arg2, int arg3, DatanodeIDInterface arg4) throws java.io.IOException;
    IOStreamPairInterface doSaslHandshake(org.apache.hadoop.hdfs.net.Peer arg0, java.io.OutputStream arg1, java.io.InputStream arg2, java.util.Map<java.lang.String, java.lang.String> arg3, javax.security.auth.callback.CallbackHandler arg4) throws java.io.IOException;
    IOStreamPairInterface getEncryptedStreams(org.apache.hadoop.hdfs.net.Peer arg0, java.io.OutputStream arg1, java.io.InputStream arg2) throws java.io.IOException;
    byte[] getEncryptionKeyFromUserName(java.lang.String arg0) throws java.io.IOException;
    char[] buildServerPassword(java.lang.String arg0) throws java.io.IOException;
    void sendInvalidKeySaslErrorMessage(java.io.DataOutputStream arg0, java.lang.String arg1) throws java.io.IOException;
    BlockTokenIdentifierInterface deserializeIdentifier(java.lang.String arg0) throws java.io.IOException;
    IOStreamPairInterface getSaslStreams(org.apache.hadoop.hdfs.net.Peer arg0, java.io.OutputStream arg1, java.io.InputStream arg2) throws java.io.IOException;
}