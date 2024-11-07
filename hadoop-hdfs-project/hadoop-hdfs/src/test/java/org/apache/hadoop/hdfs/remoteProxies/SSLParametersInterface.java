package org.apache.hadoop.hdfs.remoteProxies;

public interface SSLParametersInterface {
    void setCipherSuites(java.lang.String[] arg0);
    int getMaximumPacketSize();
    void setMaximumPacketSize(int arg0);
    boolean getUseCipherSuitesOrder();
    boolean getNeedClientAuth();
    void setSNIMatchers(java.util.Collection<javax.net.ssl.SNIMatcher> arg0);
    java.lang.String[] getProtocols();
    java.lang.String[] getCipherSuites();
    boolean getEnableRetransmissions();
    java.util.Collection<javax.net.ssl.SNIMatcher> getSNIMatchers();
    void setEnableRetransmissions(boolean arg0);
    void setWantClientAuth(boolean arg0);
    java.security.AlgorithmConstraints getAlgorithmConstraints();
    java.util.List<javax.net.ssl.SNIServerName> getServerNames();
    boolean getWantClientAuth();
    void setServerNames(java.util.List<javax.net.ssl.SNIServerName> arg0);
    java.lang.String[] clone(java.lang.String[] arg0);
    void setEndpointIdentificationAlgorithm(java.lang.String arg0);
    void setApplicationProtocols(java.lang.String[] arg0);
    void setProtocols(java.lang.String[] arg0);
    java.lang.String[] getApplicationProtocols();
    void setUseCipherSuitesOrder(boolean arg0);
    java.lang.String getEndpointIdentificationAlgorithm();
    void setNeedClientAuth(boolean arg0);
    void setAlgorithmConstraints(java.security.AlgorithmConstraints arg0);
}