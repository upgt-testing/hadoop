package org.apache.hadoop.hdfs.remoteProxies;

public interface SSLEngineResultInterface {
    int bytesProduced();
    long sequenceNumber();
    javax.net.ssl.SSLEngineResult.HandshakeStatus getHandshakeStatus();
    int bytesConsumed();
    javax.net.ssl.SSLEngineResult.Status getStatus();
    java.lang.String toString();
}