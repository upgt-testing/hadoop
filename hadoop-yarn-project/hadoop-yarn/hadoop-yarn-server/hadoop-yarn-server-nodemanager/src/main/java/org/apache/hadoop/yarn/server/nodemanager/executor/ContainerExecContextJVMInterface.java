package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerExecContextJVMInterface {

    java.lang.String getShell();

    java.lang.String getUser();

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getLocalDirsHandlerService();

    java.lang.String getAppId();

    java.lang.Object getContainer();
}
