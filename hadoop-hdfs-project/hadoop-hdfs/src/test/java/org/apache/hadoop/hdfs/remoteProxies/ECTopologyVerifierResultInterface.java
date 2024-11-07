package org.apache.hadoop.hdfs.remoteProxies;

public interface ECTopologyVerifierResultInterface {
    boolean isSupported();
    java.lang.String getResultMessage();
}