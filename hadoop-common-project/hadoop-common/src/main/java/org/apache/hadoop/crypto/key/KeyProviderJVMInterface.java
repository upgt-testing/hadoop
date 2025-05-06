package org.apache.hadoop.crypto.key;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface KeyProviderJVMInterface {
    KeyVersionJVMInterface createKey(String keyName, KeyProvider.Options options) throws NoSuchAlgorithmException, IOException;
    void flush() throws IOException;
    void deleteKey(String keyName) throws IOException;
}
