package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.crypto.key.KeyProvider;
import org.apache.hadoop.crypto.key.KeyProviderCryptoExtension;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.*;

public interface KeyProviderCryptoExtensionInterface {

    void warmUpEncryptedKeys(String keyNames);

    KeyProviderCryptoExtension.EncryptedKeyVersion generateEncryptedKey(String encryptionKeyName);

    KeyProvider.KeyVersion decryptEncryptedKey(KeyProviderCryptoExtension.EncryptedKeyVersion encryptedKey);

    KeyProviderCryptoExtension.EncryptedKeyVersion reencryptEncryptedKey(KeyProviderCryptoExtension.EncryptedKeyVersion ekv);

    void drain(String keyName);

    void reencryptEncryptedKeys(List<KeyProviderCryptoExtension.EncryptedKeyVersion> ekvs);

    void close();

    KeyProvider.KeyVersion createKey(String name, byte[] material,
                                                     KeyProvider.Options options) throws IOException;

    KeyProvider.KeyVersion createKey(String name, KeyProvider.Options options)
            throws NoSuchAlgorithmException, IOException;

    void flush() throws IOException;
    void deleteKey(String name) throws IOException;
}
