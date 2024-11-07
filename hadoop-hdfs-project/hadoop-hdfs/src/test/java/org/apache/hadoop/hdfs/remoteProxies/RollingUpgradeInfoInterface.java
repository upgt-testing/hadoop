package org.apache.hadoop.hdfs.remoteProxies;

public interface RollingUpgradeInfoInterface {
    boolean createdRollbackImages();
    boolean isFinalized();
    java.lang.String toString();
    java.lang.String getBlockPoolId();
    int hashCode();
    long getStartTime();
    void setCreatedRollbackImages(boolean arg0);
    java.lang.String timestamp2String(long arg0);
    void finalize(long arg0);
    boolean isStarted();
    long getFinalizeTime();
    boolean equals(java.lang.Object arg0);
}