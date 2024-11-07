package org.apache.hadoop.hdfs.remoteProxies;

public interface ServiceAuthorizationManagerInterface {
    java.util.Set<java.lang.Class<?>> getProtocolsWithMachineLists();
    void refresh(ConfigurationInterface arg0, PolicyProviderInterface arg1);
    AccessControlListInterface getProtocolsBlockedAcls(java.lang.Class<?> arg0);
    MachineListInterface getProtocolsMachineList(java.lang.Class<?> arg0);
    java.lang.String getHostKey(java.lang.String arg0);
    AccessControlListInterface getProtocolsAcls(java.lang.Class<?> arg0);
    void refreshWithLoadedConfiguration(ConfigurationInterface arg0, PolicyProviderInterface arg1);
    java.util.Set<java.lang.Class<?>> getProtocolsWithAcls();
    void authorize(UserGroupInformationInterface arg0, java.lang.Class<?> arg1, ConfigurationInterface arg2, java.net.InetAddress arg3) throws org.apache.hadoop.security.authorize.AuthorizationException;
    MachineListInterface getProtocolsBlockedMachineList(java.lang.Class<?> arg0);
}