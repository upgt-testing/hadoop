package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface LocalizerStartContextJVMInterface {

    java.lang.String getLocId();

    java.net.InetSocketAddress getNmAddr();

    org.apache.hadoop.fs.PathJVMInterface getNmPrivateContainerTokens();

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getDirsHandler();

    java.lang.String getUser();

    java.lang.String getAppId();
}
