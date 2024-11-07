package org.apache.hadoop.hdfs.remoteProxies;

public interface KerberosTicketInterface {
    KerberosPrincipalInterface getClient();
    boolean equals(java.lang.Object arg0);
    java.util.Date getRenewTill();
    int getSessionKeyType();
    boolean isCurrent();
    KerberosPrincipalInterface getServer();
    java.net.InetAddress[] getClientAddresses();
    boolean isForwardable();
    void init(byte[] arg0, KerberosPrincipalInterface arg1, KerberosPrincipalInterface arg2, byte[] arg3, int arg4, boolean[] arg5, java.util.Date arg6, java.util.Date arg7, java.util.Date arg8, java.util.Date arg9, java.net.InetAddress[] arg10);
    java.lang.String toString();
    java.util.Date getStartTime();
    int hashCode();
    byte[] getEncoded();
    boolean isProxiable();
    boolean isDestroyed();
    java.util.Date getEndTime();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    boolean isForwarded();
    void refresh() throws javax.security.auth.RefreshFailedException;
    void destroy() throws javax.security.auth.DestroyFailedException;
    void init(byte[] arg0, KerberosPrincipalInterface arg1, KerberosPrincipalInterface arg2, KeyImplInterface arg3, boolean[] arg4, java.util.Date arg5, java.util.Date arg6, java.util.Date arg7, java.util.Date arg8, java.net.InetAddress[] arg9);
    boolean[] getFlags();
    boolean isPostdated();
    java.util.Date getAuthTime();
    boolean isInitial();
    boolean isProxy();
    boolean isRenewable();
    javax.crypto.SecretKey getSessionKey();
}