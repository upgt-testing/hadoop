package org.apache.hadoop.hdfs.remoteProxies;

public interface AccessControlListInterface {
    java.lang.String getUsersString();
    java.util.Collection<java.lang.String> getUsers();
    boolean isUserInList(UserGroupInformationInterface arg0);
    void removeGroup(java.lang.String arg0);
    void removeUser(java.lang.String arg0);
    java.lang.String getGroupsString();
    java.lang.String getAclString();
    void addGroup(java.lang.String arg0);
    java.lang.String toString();
    boolean isWildCardACLValue(java.lang.String arg0);
    java.util.Collection<java.lang.String> getGroups();
    void buildACL(java.lang.String[] arg0);
    java.lang.String getString(java.util.Collection<java.lang.String> arg0);
    void write(java.io.DataOutput arg0) throws java.io.IOException;
    void addUser(java.lang.String arg0);
    void readFields(java.io.DataInput arg0) throws java.io.IOException;
    boolean isAllAllowed();
    boolean isUserAllowed(UserGroupInformationInterface arg0);
}