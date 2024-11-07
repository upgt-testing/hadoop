package org.apache.hadoop.hdfs.remoteProxies;

public interface CacheEntryWithPayloadInterface {
    int hashCode();
    long getExpirationTime();
    boolean isSuccess();
    int hashCode(long arg0);
    org.apache.hadoop.util.LightWeightGSet.LinkedElement getNext();
    void setExpirationTime(long arg0);
    void setNext(org.apache.hadoop.util.LightWeightGSet.LinkedElement arg0);
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    java.lang.Object getPayload();
    void completed(boolean arg0);
}