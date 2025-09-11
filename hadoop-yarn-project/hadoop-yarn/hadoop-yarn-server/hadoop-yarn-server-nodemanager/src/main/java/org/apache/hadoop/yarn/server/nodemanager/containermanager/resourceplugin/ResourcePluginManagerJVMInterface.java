package org.apache.hadoop.yarn.server.nodemanager.containermanager.resourceplugin;

public interface ResourcePluginManagerJVMInterface {

    java.util.Map getNameToPlugins();

    void initialize_bridge(java.lang.Object arg0) throws org.apache.hadoop.yarn.exceptions.YarnException;

    void cleanup() throws org.apache.hadoop.yarn.exceptions.YarnException;
}
