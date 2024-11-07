package org.apache.hadoop.hdfs.remoteProxies;

public interface CredentialEntryInterface {
    char[] getCredential();
    java.lang.String toString();
    java.lang.String getAlias();
}