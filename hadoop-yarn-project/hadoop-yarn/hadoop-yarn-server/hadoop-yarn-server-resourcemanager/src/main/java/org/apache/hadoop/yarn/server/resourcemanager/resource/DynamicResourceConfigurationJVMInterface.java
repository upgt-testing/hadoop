package org.apache.hadoop.yarn.server.resourcemanager.resource;

import org.apache.hadoop.conf.ConfigurationJVMInterface;

public interface DynamicResourceConfigurationJVMInterface extends ConfigurationJVMInterface {

    java.lang.String[] getNodes();

    java.util.Map getNodeResourceMap();

    void setVcoresPerNode(java.lang.String arg0, int arg1);

    void setOverCommitTimeoutPerNode(java.lang.String arg0, int arg1);

    int getMemoryPerNode(java.lang.String arg0);

    int getVcoresPerNode(java.lang.String arg0);

    int getOverCommitTimeoutPerNode(java.lang.String arg0);

    void setMemoryPerNode(java.lang.String arg0, int arg1);

    void setNodes(java.lang.String[] arg0);
}
