package org.apache.hadoop.yarn.api.records;

public interface TokenJVMInterface {

    java.nio.ByteBuffer getIdentifier();

    void setKind(java.lang.String arg0);

    void setService(java.lang.String arg0);

    void setIdentifier(java.nio.ByteBuffer arg0);

    java.lang.String getKind();

    java.nio.ByteBuffer getPassword();

    java.lang.String getService();

    void setPassword(java.nio.ByteBuffer arg0);
}
