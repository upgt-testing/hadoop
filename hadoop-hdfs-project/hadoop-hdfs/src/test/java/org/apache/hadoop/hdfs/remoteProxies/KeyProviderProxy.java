package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.crypto.key.KeyProvider;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface KeyProviderProxy {
    KeyProvider.KeyVersion createKey(String name, KeyProvider.Options options) throws NoSuchAlgorithmException, IOException; //Shuai: Should we change the return type to a proxy class?
    void flush() throws IOException;
    void deleteKey(String name) throws IOException;
}
