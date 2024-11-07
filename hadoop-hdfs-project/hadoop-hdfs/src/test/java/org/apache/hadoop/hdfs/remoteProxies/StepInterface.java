package org.apache.hadoop.hdfs.remoteProxies;

public interface StepInterface {
    int hashCode();
    boolean equals(java.lang.Object arg0);
    long getSize();
    org.apache.hadoop.hdfs.server.namenode.startupprogress.StepType getType();
    int compareTo(StepInterface arg0);
    java.lang.String getFile();
}