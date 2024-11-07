package org.apache.hadoop.hdfs.remoteProxies;

public interface KeyImplInterface {
    java.lang.String getAlgorithm();
    java.lang.String getFormat();
    java.lang.String toString();
    void destroy() throws javax.security.auth.DestroyFailedException;
    boolean isDestroyed();
    byte[] getEncoded();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    boolean equals(java.lang.Object arg0);
    java.lang.String getAlgorithmName(int arg0);
    int getKeyType();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    int hashCode();
}