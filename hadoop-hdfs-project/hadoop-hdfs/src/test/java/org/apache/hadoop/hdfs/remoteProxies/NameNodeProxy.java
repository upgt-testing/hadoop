package org.apache.hadoop.hdfs.remoteProxies;


public interface NameNodeProxy {
    FSNameSystemProxy getNamesystem();
    boolean testRMIPrint(String message);
    void testRMIConf(org.apache.hadoop.conf.Configuration conf);
    InetSocketAddressProxy getHttpsAddress();
    InetSocketAddressProxy getHttpAddress();
    String getHostAndPort();
    boolean isSecurityEnabled();
}
