package org.apache.hadoop.hdfs.remoteProxies;

public interface SecureResourcesInterface {
    java.net.ServerSocket getStreamingSocket();
    boolean isHttpPortPrivileged();
    java.nio.channels.ServerSocketChannel getHttpServerChannel();
    boolean isSaslEnabled();
    boolean isRpcPortPrivileged();
}