package org.apache.hadoop.hdfs.remoteProxies;

public interface LoginContextInterface {
    void login() throws javax.security.auth.login.LoginException;
    void logout() throws javax.security.auth.login.LoginException;
    void invokePriv(java.lang.String arg0) throws javax.security.auth.login.LoginException;
    void init(java.lang.String arg0) throws javax.security.auth.login.LoginException;
    void throwException(LoginExceptionInterface arg0, LoginExceptionInterface arg1) throws javax.security.auth.login.LoginException;
    void invoke(java.lang.String arg0) throws javax.security.auth.login.LoginException;
    SubjectInterface getSubject();
    void clearState();
    void loadDefaultCallbackHandler() throws javax.security.auth.login.LoginException;
}