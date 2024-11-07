package org.apache.hadoop.hdfs.remoteProxies;

public interface DiscoveredAnnotationInterface {
    java.lang.Class<?> getTargetClass();
    void loadClass();
    void apply();
    ResourceInterface getResource();
}