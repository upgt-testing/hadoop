package org.apache.hadoop.hdfs.remoteProxies;

public interface ServiceInterface {
    java.lang.Class<?> getProtocol();
    java.lang.String getServiceKey();
}