package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface LocalDirsHandlerServiceJVMInterface extends AbstractServiceJVMInterface {

    java.util.List getLogDirsForCleanup();

    org.apache.hadoop.fs.PathJVMInterface getLogPathToRead(java.lang.String arg0) throws java.io.IOException;

    void deregisterLogDirsChangeListener_bridge(java.lang.Object arg0);

    void deregisterLocalDirsChangeListener_bridge(java.lang.Object arg0);

    void registerLocalDirsChangeListener_bridge(java.lang.Object arg0);

    java.lang.Iterable getAllLocalPathsForRead(java.lang.String arg0) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface getLocalPathForRead(java.lang.String arg0) throws java.io.IOException;

    java.util.List getLocalDirsForRead();

    void registerLogDirsChangeListener_bridge(java.lang.Object arg0);

    java.util.List getDiskFullLocalDirs();

    boolean isGoodLogDir(java.lang.String arg0);

    boolean areDisksHealthy();

    java.util.List getDiskFullLogDirs();

    java.util.List getLogDirs();

    java.util.List getLogDirsForRead();

    java.util.List getLocalDirs();

    long getLastDisksCheckTime();

    java.util.List getLocalDirsForCleanup();

    org.apache.hadoop.fs.PathJVMInterface getLogPathForWrite(java.lang.String arg0, boolean arg1) throws java.io.IOException;

    boolean isGoodLocalDir(java.lang.String arg0);

    org.apache.hadoop.fs.PathJVMInterface getLocalPathForWrite(java.lang.String arg0) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface getLocalPathForWrite(java.lang.String arg0, long arg1, boolean arg2) throws java.io.IOException;

    java.lang.String getDisksHealthReport(boolean arg0);
}
