package org.apache.hadoop.hdfs.remoteProxies;

public interface CacheEntryInterface {
    void setExpirationTime(long arg0);
    int hashCode(long arg0);
    void setNext(org.apache.hadoop.util.LightWeightGSet.LinkedElement arg0);
    long getExpirationTime();
    org.apache.hadoop.util.LightWeightGSet.LinkedElement getNext();
    void completed(boolean arg0);
    boolean isSuccess();
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    int hashCode();
}