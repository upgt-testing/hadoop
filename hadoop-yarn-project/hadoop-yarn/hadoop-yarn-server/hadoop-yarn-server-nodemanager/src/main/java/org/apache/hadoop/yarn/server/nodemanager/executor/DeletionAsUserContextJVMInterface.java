package org.apache.hadoop.yarn.server.nodemanager.executor;

public interface DeletionAsUserContextJVMInterface {

    java.lang.String getUser();

    org.apache.hadoop.fs.PathJVMInterface getSubDir();

    java.util.List getBasedirs();
}
