package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface ContainerLivenessContextJVMInterface {

    java.lang.String getUser();

    java.lang.String getPid();

    java.lang.Object getContainer();
}
