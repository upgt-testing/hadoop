package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.SaslRpcServer;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.TokenIdentifier;
//import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.io.Text;

public interface UserGroupInformationInterface {

    boolean hasKerberosCredentials();

    boolean isFromKeytab();

    boolean shouldRelogin();

    void logoutUserFromKeytab();

    void checkTGTAndReloginFromKeytab();

    void reloginFromKeytab();

    void forceReloginFromKeytab();

    void reloginFromTicketCache();

    UserGroupInformationInterface getRealUser();

    String getShortUserName();

    String getPrimaryGroupName();

    String getUserName();

    boolean addTokenIdentifier(TokenIdentifierInterface tokenId);

    Set<TokenIdentifierInterface> getTokenIdentifiers();

    //boolean addToken(Token<? extends TokenIdentifier> token);

    //boolean addToken(Text alias, Token<? extends TokenIdentifier> token);

    //Collection<Token<? extends TokenIdentifier>> getTokens();

    CredentialsInterface getCredentials();

    void addCredentials(Credentials credentials);

    String[] getGroupNames();

    List<String> getGroups();

    String toString();

    void setAuthenticationMethod(UserGroupInformation.AuthenticationMethod authMethod);

    void setAuthenticationMethod(SaslRpcServer.AuthMethod authMethod);

    UserGroupInformation.AuthenticationMethod getAuthenticationMethod();

    UserGroupInformation.AuthenticationMethod getRealAuthenticationMethod();

    boolean equals(Object o);

    int hashCode();

    //T doAs(PrivilegedAction<T> action);

    //T doAs(PrivilegedExceptionAction<T> action);
}
