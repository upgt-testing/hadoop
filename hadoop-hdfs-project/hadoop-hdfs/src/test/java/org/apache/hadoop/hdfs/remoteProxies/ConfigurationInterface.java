package org.apache.hadoop.hdfs.remoteProxies;

public interface ConfigurationInterface {
    ConfigurationInterface handleException(java.security.NoSuchAlgorithmException arg0) throws java.security.NoSuchAlgorithmException;
    java.lang.String getType();
    ConfigurationInterface getInstance(java.lang.String arg0, javax.security.auth.login.Configuration.Parameters arg1, java.lang.String arg2) throws java.security.NoSuchProviderException, java.security.NoSuchAlgorithmException;
    void refresh();
    ConfigurationInterface getInstance(java.lang.String arg0, javax.security.auth.login.Configuration.Parameters arg1) throws java.security.NoSuchAlgorithmException;
    java.security.Provider getProvider();
    ConfigurationInterface getConfiguration();
    void checkPermission(java.lang.String arg0);
    void setConfiguration(ConfigurationInterface arg0);
    AppConfigurationEntryInterface[] getAppConfigurationEntry(java.lang.String arg0);
    javax.security.auth.login.Configuration.Parameters getParameters();
    ConfigurationInterface getInstance(java.lang.String arg0, javax.security.auth.login.Configuration.Parameters arg1, java.security.Provider arg2) throws java.security.NoSuchAlgorithmException;
}