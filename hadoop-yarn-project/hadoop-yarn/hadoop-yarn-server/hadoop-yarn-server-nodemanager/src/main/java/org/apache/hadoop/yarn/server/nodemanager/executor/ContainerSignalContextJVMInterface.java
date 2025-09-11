package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerSignalContextJVMInterface {

    int hashCode();

    java.lang.Object getSignal();

    boolean equals(java.lang.Object arg0);

    java.lang.String getUser();

    java.lang.String getPid();

    java.lang.Object getContainer();
}
