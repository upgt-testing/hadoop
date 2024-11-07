package org.apache.hadoop.hdfs.remoteProxies;

public interface HadoopLoginContextInterface {
    void loadDefaultCallbackHandler() throws javax.security.auth.login.LoginException;
    void clearState();
    void invokePriv(java.lang.String arg0) throws javax.security.auth.login.LoginException;
    void init(java.lang.String arg0) throws javax.security.auth.login.LoginException;
    SubjectInterface getSubject();
    void login() throws javax.security.auth.login.LoginException;
    java.lang.Object getSubjectLock();
    java.lang.String getAppName();
    void throwException(LoginExceptionInterface arg0, LoginExceptionInterface arg1) throws javax.security.auth.login.LoginException;
    void logout() throws javax.security.auth.login.LoginException;
    HadoopConfigurationInterface getConfiguration();
    void invoke(java.lang.String arg0) throws javax.security.auth.login.LoginException;
}