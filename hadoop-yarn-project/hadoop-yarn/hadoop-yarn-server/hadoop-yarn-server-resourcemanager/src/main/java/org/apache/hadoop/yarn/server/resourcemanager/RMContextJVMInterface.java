package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.yarn.event.DispatcherJVMInterface;
import org.apache.hadoop.yarn.server.resourcemanager.metrics.SystemMetricsPublisherJVMInterface;


public interface RMContextJVMInterface {
    AdminServiceJVMInterface getRMAdminService();

    DispatcherJVMInterface getDispatcher();

    boolean isHAEnabled();

    ApplicationMasterServiceJVMInterface getApplicationMasterService();

    ResourceTrackerServiceJVMInterface getResourceTrackerService();

    //void setSystemMetricsPublisher(SystemMetricsPublisher systemMetricsPublisher);

    SystemMetricsPublisherJVMInterface getSystemMetricsPublisher();


    boolean isWorkPreservingRecoveryEnabled();


    long getEpoch();

    boolean isSchedulerReadyForAllocatingContainers();

    Configuration getYarnConfiguration();

    //void setLeaderElectorService(EmbeddedElector elector);

    EmbeddedElectorJVMInterface getLeaderElectorService();

    String getHAZookeeperConnectionState();

    ResourceManagerJVMInterface getResourceManager();


}
