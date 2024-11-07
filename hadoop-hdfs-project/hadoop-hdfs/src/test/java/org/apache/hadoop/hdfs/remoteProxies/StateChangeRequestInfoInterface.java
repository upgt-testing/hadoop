package org.apache.hadoop.hdfs.remoteProxies;

public interface StateChangeRequestInfoInterface {
    org.apache.hadoop.ha.HAServiceProtocol.RequestSource getSource();
}