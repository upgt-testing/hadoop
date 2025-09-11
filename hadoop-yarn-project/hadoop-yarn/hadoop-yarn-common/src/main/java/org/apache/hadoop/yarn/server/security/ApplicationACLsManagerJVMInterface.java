package org.apache.hadoop.yarn.server.security;

public interface ApplicationACLsManagerJVMInterface {

    void addApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationAccessType, java.lang.String> arg1);

    boolean isAdmin_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0);

    boolean areACLsEnabled();

    void removeApplication_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    boolean checkAccess_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0, java.lang.Object arg1, java.lang.String arg2, org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg3);
}
