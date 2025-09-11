package org.apache.hadoop.security.authorize;

import org.apache.hadoop.io.WritableJVMInterface;

public interface AccessControlListJVMInterface extends WritableJVMInterface {

    void removeGroup(java.lang.String arg0);

    java.lang.String getAclString();

    java.lang.String toString();

    boolean isUserInList_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0);

    void removeUser(java.lang.String arg0);

    void readFields_bridge(java.lang.Object arg0) throws java.io.IOException;

    boolean isUserAllowed_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0);

    boolean isAllAllowed();

    java.util.Collection getGroups();

    java.util.Collection getUsers();

    void addGroup(java.lang.String arg0);

    void write_bridge(java.lang.Object arg0) throws java.io.IOException;

    void addUser(java.lang.String arg0);
}
