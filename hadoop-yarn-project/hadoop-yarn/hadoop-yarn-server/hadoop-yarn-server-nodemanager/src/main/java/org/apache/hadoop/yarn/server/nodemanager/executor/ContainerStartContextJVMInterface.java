package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerStartContextJVMInterface {

    java.lang.String getUser();

    org.apache.hadoop.fs.PathJVMInterface getNmPrivateContainerScriptPath();

    java.util.List getContainerLogDirs();

    java.lang.String getAppId();

    java.util.Map getLocalizedResources();

    java.util.List getLogDirs();

    org.apache.hadoop.fs.PathJVMInterface getNmPrivateTokensPath();

    java.util.List getLocalDirs();

    java.util.List getFilecacheDirs();

    java.util.List getUserLocalDirs();

    java.util.List getContainerLocalDirs();

    org.apache.hadoop.fs.PathJVMInterface getContainerWorkDir();

    java.lang.Object getContainer();
}
