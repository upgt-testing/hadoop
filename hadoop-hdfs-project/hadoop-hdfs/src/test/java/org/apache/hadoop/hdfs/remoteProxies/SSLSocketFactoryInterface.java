package org.apache.hadoop.hdfs.remoteProxies;

public interface SSLSocketFactoryInterface {
    java.lang.String[] getSupportedCipherSuites();
    java.net.Socket createSocket(java.net.Socket arg0, java.lang.String arg1, int arg2, boolean arg3) throws java.io.IOException;
    java.net.Socket createSocket(java.net.InetAddress arg0, int arg1) throws java.io.IOException;
    java.net.Socket createSocket(java.lang.String arg0, int arg1, java.net.InetAddress arg2, int arg3) throws java.io.IOException, java.net.UnknownHostException;
    java.net.Socket createSocket(java.net.InetAddress arg0, int arg1, java.net.InetAddress arg2, int arg3) throws java.io.IOException;
    java.lang.String getSecurityProperty(java.lang.String arg0);
    java.net.Socket createSocket() throws java.io.IOException;
    java.net.Socket createSocket(java.lang.String arg0, int arg1) throws java.io.IOException, java.net.UnknownHostException;
    SocketFactoryInterface getDefault();
    java.net.Socket createSocket(java.net.Socket arg0, java.io.InputStream arg1, boolean arg2) throws java.io.IOException;
    java.lang.String[] getDefaultCipherSuites();
    void log(java.lang.String arg0);
}