package org.apache.hadoop.hdfs.remoteProxies;

public interface CacheDirectiveInfoInterface {
    java.lang.Long getId();
    java.lang.Short getReplication();
    ExpirationInterface getExpiration();
    java.lang.String getPool();
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    int hashCode();
    PathInterface getPath();
}