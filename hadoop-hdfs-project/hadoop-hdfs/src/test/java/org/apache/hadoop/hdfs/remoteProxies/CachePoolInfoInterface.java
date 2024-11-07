package org.apache.hadoop.hdfs.remoteProxies;

public interface CachePoolInfoInterface {
    CachePoolInfoInterface setLimit(java.lang.Long arg0);
    java.lang.String getOwnerName();
    CachePoolInfoInterface setMode(FsPermissionInterface arg0);
    java.lang.String getPoolName();
    CachePoolInfoInterface setOwnerName(java.lang.String arg0);
    java.lang.Long getMaxRelativeExpiryMs();
    CachePoolInfoInterface setGroupName(java.lang.String arg0);
    boolean equals(java.lang.Object arg0);
    CachePoolInfoInterface setMaxRelativeExpiryMs(java.lang.Long arg0);
    FsPermissionInterface getMode();
    java.lang.Short getDefaultReplication();
    int hashCode();
    void validate(CachePoolInfoInterface arg0) throws java.io.IOException;
    java.lang.String toString();
    java.lang.Long getLimit();
    void validateName(java.lang.String arg0) throws java.io.IOException;
    java.lang.String getGroupName();
    CachePoolInfoInterface setDefaultReplication(java.lang.Short arg0);
}