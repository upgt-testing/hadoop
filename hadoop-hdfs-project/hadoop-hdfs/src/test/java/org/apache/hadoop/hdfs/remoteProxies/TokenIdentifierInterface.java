package org.apache.hadoop.hdfs.remoteProxies;

public interface TokenIdentifierInterface {
    TextInterface getKind();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    byte[] getBytes();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    UserGroupInformationInterface getUser();
    java.lang.String getTrackingId();
}