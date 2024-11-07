package org.apache.hadoop.hdfs.remoteProxies;

public interface ConfigRedactorInterface {
    java.lang.String redact(java.lang.String arg0, java.lang.String arg1);
    boolean configIsSensitive(java.lang.String arg0);
}