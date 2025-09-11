package org.apache.hadoop.yarn.api.records;

public interface ContainerLaunchContextJVMInterface {

    void setEnvironment(java.util.Map<java.lang.String, java.lang.String> arg0);

    java.util.Map getEnvironment();

    java.util.Map getLocalResources();

    void setLocalResources(java.util.Map<java.lang.String, org.apache.hadoop.yarn.api.records.LocalResource> arg0);

    void setServiceData(java.util.Map<java.lang.String, java.nio.ByteBuffer> arg0);

    java.util.Map getApplicationACLs();

    java.util.Map getServiceData();

    void setApplicationACLs(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationAccessType, java.lang.String> arg0);

    java.util.List getCommands();

    void setTokens(java.nio.ByteBuffer arg0);

    void setCommands(java.util.List<java.lang.String> arg0);

    java.nio.ByteBuffer getTokens();
}
