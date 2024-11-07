package org.apache.hadoop.hdfs.remoteProxies;

public interface PasswordInterface {
    boolean byteEquals(byte[] arg0, byte[] arg1);
    java.lang.String obfuscate(java.lang.String arg0);
    boolean check(java.lang.Object arg0);
    java.lang.String toStarString();
    java.lang.String deobfuscate(java.lang.String arg0);
    boolean equals(java.lang.Object arg0);
    CredentialInterface getCredential(java.lang.String arg0);
    boolean stringEquals(java.lang.String arg0, java.lang.String arg1);
    PasswordInterface getPassword(java.lang.String arg0, java.lang.String arg1, java.lang.String arg2);
    java.lang.String toString();
    int hashCode();
    void main(java.lang.String[] arg0);
}