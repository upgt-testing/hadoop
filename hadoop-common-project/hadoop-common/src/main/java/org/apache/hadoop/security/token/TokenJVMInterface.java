package org.apache.hadoop.security.token;

import org.apache.hadoop.io.WritableJVMInterface;

public interface TokenJVMInterface<T> extends WritableJVMInterface {

    byte[] getIdentifier();

    int hashCode();

    boolean equals(java.lang.Object arg0);

    void cancel_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    java.lang.String toString();

    void decodeFromUrlString(java.lang.String arg0) throws java.io.IOException;

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    byte[] getPassword();

    org.apache.hadoop.io.TextJVMInterface getKind();

    org.apache.hadoop.io.TextJVMInterface getService();

    T decodeIdentifier() throws java.io.IOException;

    boolean isPrivate();

    org.apache.hadoop.security.token.TokenJVMInterface privateClone_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    java.lang.String buildCacheKey();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    java.lang.String encodeToUrlString() throws java.io.IOException;

    boolean isPrivateCloneOf_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    long renew_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    void setKind_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    boolean isManaged() throws java.io.IOException;

    void setService_bridge(org.apache.hadoop.io.TextJVMInterface arg0);
}
