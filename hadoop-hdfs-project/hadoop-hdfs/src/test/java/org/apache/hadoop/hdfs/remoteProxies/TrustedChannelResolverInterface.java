package org.apache.hadoop.hdfs.remoteProxies;

public interface TrustedChannelResolverInterface {
    boolean isTrusted();
    boolean isTrusted(java.net.InetAddress arg0);
    ConfigurationInterface getConf();
    TrustedChannelResolverInterface getInstance(ConfigurationInterface arg0);
    void setConf(ConfigurationInterface arg0);
}