package org.apache.hadoop.hdfs.remoteProxies;

public interface TokenRenewerInterface {
    long renew(TokenInterface<?> arg0, ConfigurationInterface arg1) throws java.io.IOException, java.lang.InterruptedException;
    void cancel(TokenInterface<?> arg0, ConfigurationInterface arg1) throws java.io.IOException, java.lang.InterruptedException;
    boolean handleKind(TextInterface arg0);
    boolean isManaged(TokenInterface<?> arg0) throws java.io.IOException;
}