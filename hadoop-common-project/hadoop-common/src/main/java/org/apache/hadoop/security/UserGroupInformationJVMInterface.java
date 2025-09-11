package org.apache.hadoop.security;

public interface UserGroupInformationJVMInterface {

    void reloginFromKeytab() throws java.io.IOException;

    boolean addTokenIdentifier_bridge(org.apache.hadoop.security.token.TokenIdentifierJVMInterface arg0);

    boolean addToken_bridge(org.apache.hadoop.io.TextJVMInterface arg0, org.apache.hadoop.security.token.TokenJVMInterface arg1);

    java.lang.String getPrimaryGroupName() throws java.io.IOException;

    java.lang.Object getAuthenticationMethod();

    void setAuthenticationMethod_bridge(java.lang.Object arg0);

    org.apache.hadoop.security.UserGroupInformationJVMInterface getRealUser();

    void logoutUserFromKeytab() throws java.io.IOException;

    boolean isFromKeytab();

    boolean hasKerberosCredentials();

    int hashCode();

    java.lang.String[] getGroupNames();

    void checkTGTAndReloginFromKeytab() throws java.io.IOException;

    org.apache.hadoop.security.CredentialsJVMInterface getCredentials();

    <T> T doAs_bridge(java.lang.Object arg0);

    java.lang.String getUserName();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.util.Collection getTokens();

    java.util.Set getTokenIdentifiers();

    java.util.List getGroups();

    java.lang.String getShortUserName();

    java.lang.Object getRealAuthenticationMethod();

    void reloginFromTicketCache() throws java.io.IOException;

    boolean addToken_bridge(org.apache.hadoop.security.token.TokenJVMInterface arg0);

    void addCredentials_bridge(org.apache.hadoop.security.CredentialsJVMInterface arg0);
}
