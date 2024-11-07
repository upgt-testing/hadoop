package org.apache.hadoop.hdfs.remoteProxies;

public interface RoleInfoInterface {
    java.util.Set<java.lang.String> getRoles();
    void combine(RoleInfoInterface arg0);
    void setUserDataConstraint(org.eclipse.jetty.security.UserDataConstraint arg0);
    java.lang.String toString();
    boolean isForbidden();
    org.eclipse.jetty.security.UserDataConstraint getUserDataConstraint();
    void setAnyAuth(boolean arg0);
    void setChecked(boolean arg0);
    void addRole(java.lang.String arg0);
    boolean isAnyAuth();
    boolean isAnyRole();
    boolean isChecked();
    void setForbidden(boolean arg0);
    void setAnyRole(boolean arg0);
}