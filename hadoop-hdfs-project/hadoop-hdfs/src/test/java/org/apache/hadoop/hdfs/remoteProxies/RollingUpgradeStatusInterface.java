package org.apache.hadoop.hdfs.remoteProxies;

public interface RollingUpgradeStatusInterface {
    boolean equals(java.lang.Object arg0);
    boolean isFinalized();
    int hashCode();
    java.lang.String getBlockPoolId();
    java.lang.String toString();
}