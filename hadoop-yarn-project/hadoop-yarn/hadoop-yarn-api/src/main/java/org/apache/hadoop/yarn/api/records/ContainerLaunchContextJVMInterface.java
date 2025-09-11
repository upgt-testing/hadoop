package org.apache.hadoop.yarn.api.records;

public interface ContainerLaunchContextJVMInterface {

    org.apache.hadoop.yarn.api.records.ContainerRetryContextJVMInterface getContainerRetryContext();

    java.nio.ByteBuffer getTokensConf();

    java.util.Map getLocalResources();

    void setServiceData(java.util.Map<java.lang.String, java.nio.ByteBuffer> arg0);

    void setApplicationACLs(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationAccessType, java.lang.String> arg0);

    void setCommands(java.util.List<java.lang.String> arg0);

    java.nio.ByteBuffer getTokens();

    void setContainerRetryContext_bridge(org.apache.hadoop.yarn.api.records.ContainerRetryContextJVMInterface arg0);

    void setEnvironment(java.util.Map<java.lang.String, java.lang.String> arg0);

    java.util.Map getEnvironment();

    void setLocalResources(java.util.Map<java.lang.String, org.apache.hadoop.yarn.api.records.LocalResource> arg0);

    java.util.Map getApplicationACLs();

    java.util.Map getServiceData();

    void setTokensConf(java.nio.ByteBuffer arg0);

    java.util.List getCommands();

    void setTokens(java.nio.ByteBuffer arg0);
}
