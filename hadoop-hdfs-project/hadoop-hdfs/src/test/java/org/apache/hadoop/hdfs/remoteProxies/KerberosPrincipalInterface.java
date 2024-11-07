package org.apache.hadoop.hdfs.remoteProxies;

public interface KerberosPrincipalInterface {
    java.lang.String getRealm();
    boolean equals(java.lang.Object arg0);
    java.lang.String toString();
    int getNameType();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    boolean implies(SubjectInterface arg0);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    java.lang.String getName();
    int hashCode();
}