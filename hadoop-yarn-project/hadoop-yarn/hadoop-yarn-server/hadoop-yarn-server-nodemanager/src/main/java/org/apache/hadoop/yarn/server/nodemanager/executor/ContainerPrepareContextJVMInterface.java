package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerPrepareContextJVMInterface {

    java.lang.String getUser();

    java.util.Map getLocalizedResources();

    java.util.List getContainerLocalDirs();

    java.util.List getCommands();

    java.lang.Object getContainer();
}
