package org.apache.hadoop.hdfs.remoteProxies;

public interface SubjectInterface {
    <T> T doAs(SubjectInterface arg0, java.security.PrivilegedAction<T> arg1);
    java.security.AccessControlContext createContext(SubjectInterface arg0, java.security.AccessControlContext arg1);
    <T> T doAsPrivileged(SubjectInterface arg0, java.security.PrivilegedAction<T> arg1, java.security.AccessControlContext arg2);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    void collectionNullClean(java.util.Collection<?> arg0);
    SubjectInterface getSubject(java.security.AccessControlContext arg0);
    java.util.Set<java.security.Principal> getPrincipals();
    java.lang.String toString();
    <T> T doAsPrivileged(SubjectInterface arg0, java.security.PrivilegedExceptionAction<T> arg1, java.security.AccessControlContext arg2) throws java.security.PrivilegedActionException;
    boolean equals(java.lang.Object arg0);
    <T> T doAs(SubjectInterface arg0, java.security.PrivilegedExceptionAction<T> arg1) throws java.security.PrivilegedActionException;
    java.util.Set<java.lang.Object> getPrivateCredentials();
    java.util.Set<java.lang.Object> getPublicCredentials();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    boolean isReadOnly();
    <T> java.util.Set<T> getPublicCredentials(java.lang.Class<T> arg0);
    java.lang.String toString(boolean arg0);
    int hashCode();
    <T> java.util.Set<T> getPrivateCredentials(java.lang.Class<T> arg0);
    void setReadOnly();
    int getCredHashCode(java.lang.Object arg0);
    <T> java.util.Set<T> getPrincipals(java.lang.Class<T> arg0);
}