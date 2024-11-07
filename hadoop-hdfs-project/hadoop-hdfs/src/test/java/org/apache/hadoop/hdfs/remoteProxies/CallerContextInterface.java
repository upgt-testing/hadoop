package org.apache.hadoop.hdfs.remoteProxies;

public interface CallerContextInterface {
    boolean equals(java.lang.Object arg0);
    byte[] getSignature();
    boolean isContextValid();
    CallerContextInterface getCurrent();
    int hashCode();
    java.lang.String getContext();
    java.lang.String toString();
    void setCurrent(CallerContextInterface arg0);
}