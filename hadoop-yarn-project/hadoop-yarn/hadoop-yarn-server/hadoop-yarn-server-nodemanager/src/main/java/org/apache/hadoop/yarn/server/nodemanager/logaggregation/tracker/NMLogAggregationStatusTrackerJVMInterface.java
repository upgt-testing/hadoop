package org.apache.hadoop.yarn.server.nodemanager.logaggregation.tracker;

import org.apache.hadoop.service.CompositeServiceJVMInterface;

public interface NMLogAggregationStatusTrackerJVMInterface extends CompositeServiceJVMInterface {

    java.util.List pullCachedLogAggregationReports();

    void serviceStop() throws java.lang.Exception;

    void updateLogAggregationStatus_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0, java.lang.Object arg1, long arg2, java.lang.String arg3, boolean arg4);
}
