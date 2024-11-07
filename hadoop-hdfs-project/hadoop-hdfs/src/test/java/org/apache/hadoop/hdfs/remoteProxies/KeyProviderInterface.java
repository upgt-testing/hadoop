package org.apache.hadoop.hdfs.remoteProxies;

public interface KeyProviderInterface {
    void deleteKey(java.lang.String arg0) throws java.io.IOException;
    java.lang.String getAlgorithm(java.lang.String arg0);
    void close() throws java.io.IOException;
    void invalidateCache(java.lang.String arg0) throws java.io.IOException;
    MetadataInterface[] getKeysMetadata(java.lang.String... arg0) throws java.io.IOException;
    void flush() throws java.io.IOException;
    KeyVersionInterface rollNewVersion(java.lang.String arg0, byte[] arg1) throws java.io.IOException;
    java.lang.String buildVersionName(java.lang.String arg0, int arg1);
    KeyVersionInterface getKeyVersion(java.lang.String arg0) throws java.io.IOException;
    java.util.List<java.lang.String> getKeys() throws java.io.IOException;
    java.lang.String getBaseName(java.lang.String arg0) throws java.io.IOException;
    java.util.List<org.apache.hadoop.crypto.key.KeyProvider.KeyVersion> getKeyVersions(java.lang.String arg0) throws java.io.IOException;
    KeyVersionInterface rollNewVersion(java.lang.String arg0) throws java.security.NoSuchAlgorithmException, java.io.IOException;
    KeyVersionInterface getCurrentKey(java.lang.String arg0) throws java.io.IOException;
    java.lang.String noPasswordWarning();
    ConfigurationInterface getConf();
    OptionsInterface options(ConfigurationInterface arg0);
    KeyVersionInterface createKey(java.lang.String arg0, OptionsInterface arg1) throws java.security.NoSuchAlgorithmException, java.io.IOException;
    byte[] generateKey(int arg0, java.lang.String arg1) throws java.security.NoSuchAlgorithmException;
    KeyProviderInterface findProvider(java.util.List<org.apache.hadoop.crypto.key.KeyProvider> arg0, java.lang.String arg1) throws java.io.IOException;
    MetadataInterface getMetadata(java.lang.String arg0) throws java.io.IOException;
    boolean isTransient();
    java.lang.String noPasswordError();
    boolean needsPassword() throws java.io.IOException;
    KeyVersionInterface createKey(java.lang.String arg0, byte[] arg1, OptionsInterface arg2) throws java.io.IOException;
}