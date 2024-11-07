package org.apache.hadoop.hdfs.remoteProxies;

public interface TokenInterface<T> {
    void cancel(ConfigurationInterface arg0) throws java.io.IOException, java.lang.InterruptedException;
    java.lang.Class<? extends org.apache.hadoop.security.token.TokenIdentifier> getClassForIdentifier(TextInterface arg0);
    boolean isPrivate();
    T decodeIdentifier() throws java.io.IOException;
    byte[] getIdentifier();
    TextInterface getKind();
    java.lang.String encodeWritable(org.apache.hadoop.io.Writable arg0) throws java.io.IOException;
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    TokenInterface<T> privateClone(TextInterface arg0);
    void setID(byte[] arg0);
    void decodeWritable(org.apache.hadoop.io.Writable arg0, java.lang.String arg1) throws java.io.IOException;
    TokenRenewerInterface getRenewer() throws java.io.IOException;
    boolean isPrivateCloneOf(TextInterface arg0);
    long renew(ConfigurationInterface arg0) throws java.io.IOException, java.lang.InterruptedException;
    void setKind(TextInterface arg0);
    void setPassword(byte[] arg0);
    void decodeFromUrlString(java.lang.String arg0) throws java.io.IOException;
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void identifierToString(java.lang.StringBuilder arg0);
    byte[] getPassword();
    java.lang.String encodeToUrlString() throws java.io.IOException;
    int hashCode();
    TokenInterface<T> copyToken();
    boolean isManaged() throws java.io.IOException;
    java.lang.String toString();
    java.lang.String buildCacheKey();
    TextInterface getService();
    boolean equals(java.lang.Object arg0);
    void setService(TextInterface arg0);
    void addBinaryBuffer(java.lang.StringBuilder arg0, byte[] arg1);
}