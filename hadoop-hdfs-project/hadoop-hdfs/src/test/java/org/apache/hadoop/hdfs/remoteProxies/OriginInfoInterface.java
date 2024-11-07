package org.apache.hadoop.hdfs.remoteProxies;

public interface OriginInfoInterface {
    DescriptorInterface getDescriptor();
    java.lang.String getName();
    org.eclipse.jetty.webapp.Origin getOriginType();
    java.lang.String toString();
}