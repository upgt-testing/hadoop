package org.apache.hadoop.hdfs.remoteProxies;

public interface DelegationTokenIdentifierInterface {
    java.lang.String stringifyToken(TokenInterface<?> arg0) throws java.io.IOException;
    java.lang.String getTrackingId();
    int hashCode();
    UserGroupInformationInterface getUser();
    void setRenewer(TextInterface arg0);
    int getMasterKeyId();
    void setOwner(TextInterface arg0);
    void setRealUser(TextInterface arg0);
    TextInterface getKind();
    TextInterface getOwner();
    int getSequenceNumber();
    long getIssueDate();
    boolean isEqual(java.lang.Object arg0, java.lang.Object arg1);
    TextInterface getRealUser();
    void setIssueDate(long arg0);
    void setMaxDate(long arg0);
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    java.lang.String toStringStable();
    byte[] getBytes();
    void setMasterKeyId(int arg0);
    boolean equals(java.lang.Object arg0);
    TextInterface getRenewer();
    long getMaxDate();
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    void writeImpl(java.io.DataOutput arg0) throws java.io.IOException;
    java.lang.String toString();
    void clearCache();
    void setSequenceNumber(int arg0);
}