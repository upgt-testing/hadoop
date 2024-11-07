package org.apache.hadoop.hdfs.remoteProxies;

public interface SocketFactoryInterface {
    java.net.Socket createSocket(java.net.InetAddress arg0, int arg1, java.net.InetAddress arg2, int arg3) throws java.io.IOException;
    java.net.Socket createSocket(java.lang.String arg0, int arg1, java.net.InetAddress arg2, int arg3) throws java.io.IOException, java.net.UnknownHostException;
    java.net.Socket createSocket() throws java.io.IOException;
    java.net.Socket createSocket(java.lang.String arg0, int arg1) throws java.io.IOException, java.net.UnknownHostException;
    java.net.Socket createSocket(java.net.InetAddress arg0, int arg1) throws java.io.IOException;
    SocketFactoryInterface getDefault();
}