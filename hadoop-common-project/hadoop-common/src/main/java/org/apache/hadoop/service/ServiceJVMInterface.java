package org.apache.hadoop.service;

public interface ServiceJVMInterface {

    java.lang.Object getServiceState();

    java.lang.Object getFailureState();

    java.util.Map getBlockers();

    java.util.List getLifecycleHistory();

    void stop();

    void start();

    void unregisterServiceListener_bridge(java.lang.Object arg0);

    void registerServiceListener_bridge(java.lang.Object arg0);

    boolean waitForServiceToStop(long arg0);

    org.apache.hadoop.conf.ConfigurationJVMInterface getConfig();

    long getStartTime();

    void init_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    java.lang.String getName();

    boolean isInState_bridge(java.lang.Object arg0);

    java.lang.Throwable getFailureCause();

    void close() throws java.io.IOException;
}
