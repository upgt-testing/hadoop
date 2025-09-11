package org.apache.hadoop.security.token;

import org.apache.hadoop.io.WritableJVMInterface;

public interface TokenJVMInterface<T> extends WritableJVMInterface {

    void setPassword(byte[] arg0);

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getKind();

    byte[] getPassword();

    T decodeIdentifier() throws java.io.IOException;

    java.lang.String buildCacheKey();

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    boolean isPrivateCloneOf_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    boolean isManaged() throws java.io.IOException;

    void setKind_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    java.lang.Object toTokenProto();

    void setService_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    byte[] getIdentifier();

    int hashCode();

    void cancel_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    void decodeFromUrlString(java.lang.String arg0) throws java.io.IOException;

    org.apache.hadoop.io.TextJVMInterface getService();

    void setID(byte[] arg0);

    boolean isPrivate();

    org.apache.hadoop.security.token.TokenJVMInterface copyToken();

    org.apache.hadoop.security.token.TokenJVMInterface privateClone_bridge(org.apache.hadoop.io.TextJVMInterface arg0);

    java.lang.String encodeToUrlString() throws java.io.IOException;

    long renew_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;
}
