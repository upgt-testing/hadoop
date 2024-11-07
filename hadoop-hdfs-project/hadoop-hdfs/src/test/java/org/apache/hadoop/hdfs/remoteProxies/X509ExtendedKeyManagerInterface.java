package org.apache.hadoop.hdfs.remoteProxies;

public interface X509ExtendedKeyManagerInterface {
    java.lang.String[] getClientAliases(java.lang.String arg0, java.security.Principal[] arg1);
    java.lang.String chooseServerAlias(java.lang.String arg0, java.security.Principal[] arg1, java.net.Socket arg2);
    java.security.cert.X509Certificate[] getCertificateChain(java.lang.String arg0);
    java.lang.String[] getServerAliases(java.lang.String arg0, java.security.Principal[] arg1);
    java.security.PrivateKey getPrivateKey(java.lang.String arg0);
    java.lang.String chooseClientAlias(java.lang.String[] arg0, java.security.Principal[] arg1, java.net.Socket arg2);
    java.lang.String chooseEngineServerAlias(java.lang.String arg0, java.security.Principal[] arg1, SSLEngineInterface arg2);
    java.lang.String chooseEngineClientAlias(java.lang.String[] arg0, java.security.Principal[] arg1, SSLEngineInterface arg2);
}