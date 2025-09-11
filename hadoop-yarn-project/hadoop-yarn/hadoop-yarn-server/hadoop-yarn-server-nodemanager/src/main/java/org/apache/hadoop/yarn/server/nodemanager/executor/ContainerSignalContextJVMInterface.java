package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerSignalContextJVMInterface {

    java.lang.Object getSignal();

    java.lang.String getUser();

    java.lang.String getPid();

    java.lang.Object getContainer();
}
