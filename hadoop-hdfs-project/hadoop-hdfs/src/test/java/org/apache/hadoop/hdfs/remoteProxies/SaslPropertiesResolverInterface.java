package org.apache.hadoop.hdfs.remoteProxies;

public interface SaslPropertiesResolverInterface {
    java.util.Map<java.lang.String, java.lang.String> getDefaultProperties();
    java.util.Map<java.lang.String, java.lang.String> getSaslProperties(ConfigurationInterface arg0, java.lang.String arg1, org.apache.hadoop.security.SaslRpcServer.QualityOfProtection arg2);
    SaslPropertiesResolverInterface getInstance(ConfigurationInterface arg0);
    void setConf(ConfigurationInterface arg0);
    java.util.Map<java.lang.String, java.lang.String> getClientProperties(java.net.InetAddress arg0);
    java.util.Map<java.lang.String, java.lang.String> getServerProperties(java.net.InetAddress arg0);
    ConfigurationInterface getConf();
    java.util.Map<java.lang.String, java.lang.String> getClientProperties(java.net.InetAddress arg0, int arg1);
    java.util.Map<java.lang.String, java.lang.String> getServerProperties(java.net.InetAddress arg0, int arg1);
}