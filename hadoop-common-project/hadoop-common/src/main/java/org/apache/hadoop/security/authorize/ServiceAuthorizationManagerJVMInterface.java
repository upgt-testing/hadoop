package org.apache.hadoop.security.authorize;

public interface ServiceAuthorizationManagerJVMInterface {

    org.apache.hadoop.security.authorize.AccessControlListJVMInterface getProtocolsBlockedAcls(java.lang.Class<?> arg0);

    java.util.Set getProtocolsWithAcls();

    org.apache.hadoop.util.MachineListJVMInterface getProtocolsMachineList(java.lang.Class<?> arg0);

    void refreshWithLoadedConfiguration_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0, org.apache.hadoop.security.authorize.PolicyProviderJVMInterface arg1);

    void refresh_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0, org.apache.hadoop.security.authorize.PolicyProviderJVMInterface arg1);

    java.util.Set getProtocolsWithMachineLists();

    org.apache.hadoop.util.MachineListJVMInterface getProtocolsBlockedMachineList(java.lang.Class<?> arg0);

    void authorize_bridge(org.apache.hadoop.security.UserGroupInformationJVMInterface arg0, java.lang.Class<?> arg1, org.apache.hadoop.conf.ConfigurationJVMInterface arg2, java.net.InetAddress arg3) throws org.apache.hadoop.security.authorize.AuthorizationException;

    org.apache.hadoop.security.authorize.AccessControlListJVMInterface getProtocolsAcls(java.lang.Class<?> arg0);
}
