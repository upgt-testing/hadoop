package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockTokenIdentifierInterface {
    void setExpiryDate(long arg0);
    java.lang.String getBlockPoolId();
    boolean isEqual(java.lang.Object arg0, java.lang.Object arg1);
    long getExpiryDate();
    int getKeyId();
    TextInterface getKind();
    byte[] getHandshakeMsg();
    boolean equals(java.lang.Object arg0);
    java.lang.String getTrackingId();
    long getBlockId();
    void readFieldsProtobuf(java.io.DataInput arg0) throws java.io.IOException;
    int hashCode();
    org.apache.hadoop.fs.StorageType[] getStorageTypes();
    void setKeyId(int arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void readFieldsLegacy(java.io.DataInput arg0) throws java.io.IOException;
    void writeLegacy(java.io.DataOutput arg0) throws java.io.IOException;
    byte[] getBytes();
    java.lang.String toString();
    UserGroupInformationInterface getUser();
    void writeProtobuf(java.io.DataOutput arg0) throws java.io.IOException;
    java.util.EnumSet<org.apache.hadoop.hdfs.security.token.block.BlockTokenIdentifier.AccessMode> getAccessModes();
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void setHandshakeMsg(byte[] arg0);
    java.lang.String[] getStorageIds();
    java.lang.String getUserId();
}