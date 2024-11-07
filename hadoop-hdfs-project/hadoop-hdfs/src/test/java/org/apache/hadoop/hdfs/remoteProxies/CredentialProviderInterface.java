package org.apache.hadoop.hdfs.remoteProxies;

public interface CredentialProviderInterface {
    boolean isTransient();
    void flush() throws java.io.IOException;
    void deleteCredentialEntry(java.lang.String arg0) throws java.io.IOException;
    CredentialEntryInterface getCredentialEntry(java.lang.String arg0) throws java.io.IOException;
    java.util.List<java.lang.String> getAliases() throws java.io.IOException;
    boolean needsPassword() throws java.io.IOException;
    java.lang.String noPasswordWarning();
    java.lang.String noPasswordError();
    CredentialEntryInterface createCredentialEntry(java.lang.String arg0, char[] arg1) throws java.io.IOException;
}