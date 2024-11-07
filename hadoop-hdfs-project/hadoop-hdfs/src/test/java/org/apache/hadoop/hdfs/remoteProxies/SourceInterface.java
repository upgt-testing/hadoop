package org.apache.hadoop.hdfs.remoteProxies;

public interface SourceInterface {
    java.lang.String toString();
    org.eclipse.jetty.servlet.Source.Origin getOrigin();
    java.lang.String getResource();
}