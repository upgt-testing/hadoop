package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.service.api.records.PlacementConstraint;

public interface SchedulingRequestInterface {

    long getAllocationRequestId();

    void setAllocationRequestId(long allocationRequestId);

    PriorityInterface getPriority();

    //void setPriority(Priority priority);

    ExecutionTypeRequestInterface getExecutionType();

    //void setExecutionType(ExecutionTypeRequest executionType);

    Set<String> getAllocationTags();

    void setAllocationTags(Set<String> allocationTags);

    ResourceSizingInterface getResourceSizing();

    //void setResourceSizing(ResourceSizing resourceSizing);

    PlacementConstraintInterface getPlacementConstraint();

    //void setPlacementConstraint(PlacementConstraint placementConstraint);
}
