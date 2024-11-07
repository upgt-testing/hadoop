package org.apache.hadoop.hdfs.remoteProxies;

public interface HadoopConfigurationInterface {
    java.security.Provider getProvider();
    java.lang.String prependFileAuthority(java.lang.String arg0);
    javax.security.auth.login.Configuration.Parameters getParameters();
    void checkPermission(java.lang.String arg0);
    AppConfigurationEntryInterface getKerberosEntry();
    ConfigurationInterface getInstance(java.lang.String arg0, javax.security.auth.login.Configuration.Parameters arg1) throws java.security.NoSuchAlgorithmException;
    void refresh();
    ConfigurationInterface handleException(java.security.NoSuchAlgorithmException arg0) throws java.security.NoSuchAlgorithmException;
//    LoginParamsInterface getParameters();
    AppConfigurationEntryInterface[] getAppConfigurationEntry(java.lang.String arg0);
    ConfigurationInterface getConfiguration();
    ConfigurationInterface getInstance(java.lang.String arg0, javax.security.auth.login.Configuration.Parameters arg1, java.lang.String arg2) throws java.security.NoSuchProviderException, java.security.NoSuchAlgorithmException;
    java.lang.String getType();
    ConfigurationInterface getInstance(java.lang.String arg0, javax.security.auth.login.Configuration.Parameters arg1, java.security.Provider arg2) throws java.security.NoSuchAlgorithmException;
    void setConfiguration(ConfigurationInterface arg0);
}