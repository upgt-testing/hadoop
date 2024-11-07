package org.apache.hadoop.hdfs.remoteProxies;

public interface HostConfigManagerInterface {
    boolean isIncluded(DatanodeIDInterface arg0);
    java.lang.String getUpgradeDomain(DatanodeIDInterface arg0);
    java.lang.Iterable<java.net.InetSocketAddress> getExcludes();
    long getMaintenanceExpirationTimeInMS(DatanodeIDInterface arg0);
    void refresh() throws java.io.IOException;
    java.lang.Iterable<java.net.InetSocketAddress> getIncludes();
    ConfigurationInterface getConf();
    void setConf(ConfigurationInterface arg0);
    boolean isExcluded(DatanodeIDInterface arg0);
}