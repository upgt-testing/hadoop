package org.apache.hadoop.hdfs.remoteProxies;

public interface ExpirationInterface {
    ExpirationInterface newAbsolute(long arg0);
    ExpirationInterface newAbsolute(java.util.Date arg0);
    ExpirationInterface newRelative(long arg0);
    java.lang.String toString();
    long getMillis();
    boolean isRelative();
    long getAbsoluteMillis();
    java.util.Date getAbsoluteDate();
}