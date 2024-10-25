package org.apache.hadoop.hdfs;

// RemoteInterfaces.java


import org.apache.hadoop.conf.Configuration;

// Package-private interfaces
interface NameNodeProxy {
    FSNameSystemProxy getNamesystem() throws Exception;
    KeyProviderProxy getKeyProvider() throws Exception;
    boolean testRMIPrint(String message);
    void testRMIConf(org.apache.hadoop.conf.Configuration conf);
}

interface FSNameSystemProxy {
    boolean testRMIPrint(String message);
    ProviderProxy getProvider() throws Exception;
}

interface DataNodeProxy {
    boolean testRMIPrint(String message);
}

interface ProviderProxy {
    void createKey(String keyName, OptionsProxy options) throws Exception;
    void flush() throws Exception;
}

interface KeyProviderProxy {
    OptionsProxy options(Configuration conf) throws Exception;
}

interface OptionsProxy {
    void setDescription(String description) throws Exception;
    void setBitLength(int bitLength) throws Exception;
}