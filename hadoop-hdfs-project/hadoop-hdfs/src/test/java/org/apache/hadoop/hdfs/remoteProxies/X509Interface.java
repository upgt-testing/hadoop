package org.apache.hadoop.hdfs.remoteProxies;

public interface X509Interface {
    java.net.InetAddress toInetAddress(java.lang.String arg0);
    void addAddress(java.lang.String arg0);
    java.lang.String toString();
    void addName(java.lang.String arg0);
    java.security.cert.X509Certificate getCertificate();
    boolean containsAtLeastTwoColons(java.lang.String arg0);
    java.util.Set<java.lang.String> getHosts();
    java.util.Set<java.lang.String> getWilds();
    boolean isCertSign(java.security.cert.X509Certificate arg0);
    java.lang.String getAlias();
    boolean matches(java.lang.String arg0);
    boolean seemsIPAddress(java.lang.String arg0);
}