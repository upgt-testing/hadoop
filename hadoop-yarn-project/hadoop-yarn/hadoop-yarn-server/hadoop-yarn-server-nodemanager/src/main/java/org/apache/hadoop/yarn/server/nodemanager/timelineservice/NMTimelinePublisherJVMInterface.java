package org.apache.hadoop.yarn.server.nodemanager.timelineservice;

import org.apache.hadoop.service.CompositeServiceJVMInterface;

public interface NMTimelinePublisherJVMInterface extends CompositeServiceJVMInterface {

    void reportContainerResourceUsage_bridge(java.lang.Object arg0, java.lang.Long arg1, java.lang.Float arg2);

    void publishLocalizationEvent_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.event.LocalizationEventJVMInterface arg0);

    void stopTimelineClient_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    void publishContainerEvent_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.container.ContainerEventJVMInterface arg0);

    void publishApplicationEvent_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.application.ApplicationEventJVMInterface arg0);

    void setTimelineServiceAddress_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.lang.String arg1);

    void createTimelineClient_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);
}
