package org.apache.hadoop.yarn.server.resourcemanager;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.yarn.event.DispatcherJVMInterface;
import org.apache.hadoop.yarn.server.resourcemanager.metrics.SystemMetricsPublisherJVMInterface;

import org.apache.hadoop.yarn.server.resourcemanager.resource.ResourceProfilesManagerJVMInterface;


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

    ResourceProfilesManagerJVMInterface getResourceProfilesManager();


    long getTokenSequenceNo();

    void incrTokenSequenceNo();

}
