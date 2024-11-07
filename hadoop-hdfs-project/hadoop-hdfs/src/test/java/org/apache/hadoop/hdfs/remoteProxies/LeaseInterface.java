package org.apache.hadoop.hdfs.remoteProxies;

public interface LeaseInterface {
    java.util.Collection<java.lang.Long> getFiles();
    java.lang.String getHolder();
    boolean expiredHardLimit(long arg0);
    boolean expiredHardLimit();
    boolean hasFiles();
    long getLastUpdate();
    boolean expiredSoftLimit();
    java.lang.String toString();
    boolean removeFile(long arg0);
    int hashCode();
    void renew();
}