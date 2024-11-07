package org.apache.hadoop.hdfs.remoteProxies;

public interface ReconfigurationUtilInterface {
    java.util.Collection<org.apache.hadoop.conf.ReconfigurationUtil.PropertyChange> getChangedProperties(ConfigurationInterface arg0, ConfigurationInterface arg1);
    java.util.Collection<org.apache.hadoop.conf.ReconfigurationUtil.PropertyChange> parseChangedProperties(ConfigurationInterface arg0, ConfigurationInterface arg1);
}