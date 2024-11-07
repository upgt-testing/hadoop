package org.apache.hadoop.hdfs.remoteProxies;

public interface DeprecatedKeyInfoInterface {
    java.lang.String getWarningMessage(java.lang.String arg0, java.lang.String arg1);
    void clearAccessed();
    boolean getAndSetAccessed();
    java.lang.String getWarningMessage(java.lang.String arg0);
}