package org.apache.hadoop.hdfs.remoteProxies;

public interface CredentialInterface {
    boolean check(java.lang.Object arg0);
    CredentialInterface getCredential(java.lang.String arg0);
    boolean stringEquals(java.lang.String arg0, java.lang.String arg1);
    boolean byteEquals(byte[] arg0, byte[] arg1);
}