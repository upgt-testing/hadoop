package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface LocalizerStartContextJVMInterface {

    java.net.InetSocketAddress getNmAddr();

    java.lang.String getLocId();

    org.apache.hadoop.fs.PathJVMInterface getNmPrivateContainerTokens();

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getDirsHandler();

    java.lang.String getUser();

    java.lang.String getAppId();
}
