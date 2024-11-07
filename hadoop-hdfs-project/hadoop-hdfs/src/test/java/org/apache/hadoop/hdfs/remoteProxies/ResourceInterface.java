package org.apache.hadoop.hdfs.remoteProxies;

public interface ResourceInterface {
    boolean isParserRestricted();
    java.lang.Object getResource();
    java.lang.String getName();
    java.lang.String toString();
    boolean getRestrictParserDefault(java.lang.Object arg0);
}