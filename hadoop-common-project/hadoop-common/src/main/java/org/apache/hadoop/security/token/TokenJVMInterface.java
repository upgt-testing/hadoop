package org.apache.hadoop.security.token;

import org.apache.hadoop.io.WritableJVMInterface;

public interface TokenJVMInterface<T> extends WritableJVMInterface {

    int hashCode();

    byte[] getIdentifier();

    void setPassword(byte[] arg0);

    void cancel_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    void decodeFromUrlString(java.lang.String arg0) throws java.io.IOException;

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    byte[] getPassword();

    org.apache.hadoop.io.TextJVMInterface getService();

    T decodeIdentifier() throws java.io.IOException;

    void setID(byte[] arg0);

    boolean isPrivate();

    org.apache.hadoop.security.token.TokenJVMInterface copyToken();

    org.apache.hadoop.security.token.TokenJVMInterface privateClone_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    java.lang.String buildCacheKey();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.String encodeToUrlString() throws java.io.IOException;

    boolean isPrivateCloneOf_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    long renew_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    boolean isManaged() throws java.io.IOException;

    void setKind_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    void setService_bridge(org.apache.hadoop.io.TextJVMInterface arg0);
}
