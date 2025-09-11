package org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin.deviceframework;

public interface DeviceMappingManagerJVMInterface {

    java.lang.Object assignDevices_bridge(java.lang.String arg0, java.lang.Object arg1) throws org.apache.hadoop.yarn.server.nodemanager.containermanager.linux.resources.ResourceHandlerException;

    java.util.Map getAllAllowedDevices();

    int getAvailableDevices(java.lang.String arg0);

    java.util.Set getAllocatedDevices_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg1);

    void recoverAssignedDevices_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg1) throws org.apache.hadoop.yarn.server.nodemanager.containermanager.linux.resources.ResourceHandlerException;

    void addDeviceSet(java.lang.String arg0, java.util.Set<org.apache.hadoop.yarn.server.nodemanager.api.deviceplugin.Device> arg1);

    java.util.Map getAllUsedDevices();

    void addDevicePluginScheduler_bridge(java.lang.String arg0, java.lang.Object arg1);

    void cleanupAssignedDevices_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg1);

    java.util.Map getDevicePluginSchedulers();
}
