package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.crypto.CipherSuite;
import org.apache.hadoop.crypto.CryptoProtocolVersion;

public interface FileEncryptionInfoInterface {

    CipherSuite getCipherSuite();

    CryptoProtocolVersion getCryptoProtocolVersion();

    byte[] getEncryptedDataEncryptionKey();

    byte[] getIV();

    String getKeyName();

    String getEzKeyVersionName();

    String toString();

    String toStringStable();
}
