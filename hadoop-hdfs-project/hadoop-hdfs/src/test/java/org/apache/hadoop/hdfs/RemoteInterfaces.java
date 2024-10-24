package org.apache.hadoop.hdfs;

// RemoteInterfaces.java

interface ClusterInterface {
    NameNodeInterface getNameNode(int idx) throws Exception;
}

// Package-private interfaces
interface NameNodeInterface {
    NameSystemInterface getNamesystem() throws Exception;
    KeyProviderInterface getKeyProvider() throws Exception;
    boolean testRMIPrint(String message);
    void testRMIConf(org.apache.hadoop.conf.Configuration conf);
}

interface NameSystemInterface {
    ProviderInterface getProvider() throws Exception;
}

interface ProviderInterface {
    void createKey(String keyName, OptionsInterface options) throws Exception;
    void flush() throws Exception;
}

interface KeyProviderInterface {
    OptionsInterface options(Configuration conf) throws Exception;
}

interface OptionsInterface {
    void setDescription(String description) throws Exception;
    void setBitLength(int bitLength) throws Exception;
}

// Configuration class (if needed)
class Configuration {
    // Configuration details
}

