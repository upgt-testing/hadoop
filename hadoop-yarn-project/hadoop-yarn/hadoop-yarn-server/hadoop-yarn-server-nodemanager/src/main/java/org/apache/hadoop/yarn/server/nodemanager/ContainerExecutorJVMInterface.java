package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.conf.ConfigurableJVMInterface;

public interface ContainerExecutorJVMInterface extends ConfigurableJVMInterface {

    void symLink(java.lang.String arg0, java.lang.String arg1) throws java.io.IOException;

    void startLocalizer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.LocalizerStartContextJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    void init_bridge(java.lang.Object arg0) throws java.io.IOException;

    org.apache.hadoop.fs.PathJVMInterface localizeClasspathJar_bridge(org.apache.hadoop.fs.PathJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1, java.lang.String arg2) throws java.io.IOException;

    void writeLaunchEnv_bridge(java.io.OutputStream arg0, java.util.Map<java.lang.String, java.lang.String> arg1, java.util.Map<org.apache.hadoop.fs.Path, java.util.List<java.lang.String>> arg2, java.util.List<java.lang.String> arg3, org.apache.hadoop.fs.PathJVMInterface arg4, java.lang.String arg5, java.lang.String arg6, java.util.LinkedHashSet<java.lang.String> arg7) throws java.io.IOException;

    void stop();

    void updateYarnSysFS_bridge(java.lang.Object arg0, java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws java.io.IOException;

    void deleteAsUser_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.DeletionAsUserContextJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    boolean signalContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerSignalContextJVMInterface arg0) throws java.io.IOException;

    void writeLaunchEnv_bridge(java.io.OutputStream arg0, java.util.Map<java.lang.String, java.lang.String> arg1, java.util.Map<org.apache.hadoop.fs.Path, java.util.List<java.lang.String>> arg2, java.util.List<java.lang.String> arg3, org.apache.hadoop.fs.PathJVMInterface arg4, java.lang.String arg5, java.util.LinkedHashSet<java.lang.String> arg6) throws java.io.IOException;

    boolean reapContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerReapContextJVMInterface arg0) throws java.io.IOException;

    java.lang.String[] getIpAndHost_bridge(java.lang.Object arg0) throws org.apache.hadoop.yarn.server.nodemanager.containermanager.runtime.ContainerExecutionException;

    void cleanupBeforeRelaunch_bridge(java.lang.Object arg0) throws java.io.IOException, java.lang.InterruptedException;

    java.util.Map getLocalResources_bridge(java.lang.Object arg0) throws java.io.IOException;

    int reacquireContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerReacquisitionContextJVMInterface arg0) throws java.io.IOException, java.lang.InterruptedException;

    void prepareContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerPrepareContextJVMInterface arg0) throws java.io.IOException;

    java.lang.String getProcessId_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void pauseContainer_bridge(java.lang.Object arg0);

    org.apache.hadoop.hdfs.protocol.datatransfer.IOStreamPairJVMInterface execContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerExecContextJVMInterface arg0) throws org.apache.hadoop.yarn.server.nodemanager.containermanager.runtime.ContainerExecutionException;

    boolean isContainerAlive_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerLivenessContextJVMInterface arg0) throws java.io.IOException;

    int launchContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerStartContextJVMInterface arg0) throws java.io.IOException, org.apache.hadoop.yarn.exceptions.ConfigurationException;

    org.apache.hadoop.conf.ConfigurationJVMInterface getConf();

    void activateContainer_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0, org.apache.hadoop.fs.PathJVMInterface arg1);

    void start();

    void resumeContainer_bridge(java.lang.Object arg0);

    int relaunchContainer_bridge(org.apache.hadoop.yarn.server.nodemanager.executor.ContainerStartContextJVMInterface arg0) throws java.io.IOException, org.apache.hadoop.yarn.exceptions.ConfigurationException;

    void deactivateContainer_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    void setConf_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    java.lang.String getExposedPorts_bridge(java.lang.Object arg0) throws org.apache.hadoop.yarn.server.nodemanager.containermanager.runtime.ContainerExecutionException;
}
