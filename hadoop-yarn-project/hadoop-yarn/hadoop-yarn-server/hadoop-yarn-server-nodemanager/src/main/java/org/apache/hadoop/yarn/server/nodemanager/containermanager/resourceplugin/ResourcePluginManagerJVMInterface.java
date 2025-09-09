package org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin;

public interface ResourcePluginManagerJVMInterface {

    void setDeviceMappingManager_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.deviceframework.DeviceMappingManagerJVMInterface arg0);

    java.util.Map getNameToPlugins();

    void initialize_bridge(java.lang.Object arg0) throws org.apache.hadoop.yarn.exceptions.YarnException, java.lang.ClassNotFoundException;

    boolean isConfiguredResourceName(java.lang.String arg0);

    void cleanup() throws org.apache.hadoop.yarn.exceptions.YarnException;

    void checkInterfaceCompatibility(java.lang.Class<?> arg0, java.lang.Class<?> arg1) throws org.apache.hadoop.yarn.exceptions.YarnRuntimeException;

    org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.deviceframework.DeviceMappingManagerJVMInterface getDeviceMappingManager();

    void initializePluggableDevicePlugins_bridge(java.lang.Object arg0, org.apache.hadoop.conf.ConfigurationJVMInterface arg1, java.util.Map<java.lang.String, org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.ResourcePlugin> arg2) throws org.apache.hadoop.yarn.exceptions.YarnRuntimeException, java.lang.ClassNotFoundException;
}
