package org.apache.hadoop.hdfs.remoteProxies;

public interface VolumePairInterface {
    boolean equals(java.lang.Object arg0);
    java.lang.String getDestVolBasePath();
    java.lang.String getDestVolUuid();
    int hashCode();
    java.lang.String getSourceVolBasePath();
    java.lang.String getSourceVolUuid();
}