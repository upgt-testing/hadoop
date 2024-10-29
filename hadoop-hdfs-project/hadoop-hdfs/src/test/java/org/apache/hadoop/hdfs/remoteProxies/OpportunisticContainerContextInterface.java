package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.service.api.records.Resource;

public interface OpportunisticContainerContextInterface {

    //AllocationParams getAppParams();

    //ContainerIdGenerator getContainerIdGenerator();

    //void setContainerIdGenerator(ContainerIdGenerator containerIdGenerator);

    //Map<String, RemoteNode> getNodeMap();

    //void updateNodeList(List<RemoteNode> newNodeList);

    //void updateAllocationParams(Resource minResource, Resource maxResource, Resource incrResource, int containerTokenExpiryInterval);

    Set<String> getBlacklist();

    //TreeMap<SchedulerRequestKey, Map<Resource, EnrichedResourceRequest>> getOutstandingOpReqs();

    //void addToOutstandingReqs(List<ResourceRequest> resourceAsks);

    //void matchAllocationToOutstandingRequest(Resource capability, List<Allocation> allocations);

    OpportunisticSchedulerMetricsInterface getOppSchedulerMetrics();
}
