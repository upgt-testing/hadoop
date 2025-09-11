package org.apache.hadoop.service;

public interface AbstractServiceJVMInterface extends ServiceJVMInterface {

    java.lang.Object getServiceState();

    java.lang.Object getFailureState();

    java.lang.String toString();

    java.util.Map getBlockers();

    java.util.List getLifecycleHistory();

    void stop();

    void init(org.apache.hadoop.conf.Configuration arg0);

    void start();

    void unregisterServiceListener_bridge(java.lang.Object arg0);

    void registerServiceListener_bridge(java.lang.Object arg0);

    boolean waitForServiceToStop(long arg0);

    org.apache.hadoop.conf.Configuration getConfig();

    long getStartTime();

    void init_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    void removeBlocker(java.lang.String arg0);

    java.lang.String getName();

    boolean isInState_bridge(java.lang.Object arg0);

    java.lang.Throwable getFailureCause();

    void close() throws java.io.IOException;
}
