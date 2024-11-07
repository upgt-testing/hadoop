package org.apache.hadoop.hdfs.remoteProxies;

public interface ServerSocketFactoryInterface {
    java.net.ServerSocket createServerSocket(int arg0, int arg1) throws java.io.IOException;
    java.net.ServerSocket createServerSocket() throws java.io.IOException;
    java.net.ServerSocket createServerSocket(int arg0) throws java.io.IOException;
    java.net.ServerSocket createServerSocket(int arg0, int arg1, java.net.InetAddress arg2) throws java.io.IOException;
    ServerSocketFactoryInterface getDefault();
}