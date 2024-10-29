package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
//import org.apache.hadoop.yarn.service.api.records.Resource;

public interface ApplicationResourceUsageReportInterface {

    int getNumUsedContainers();

    void setNumUsedContainers(int num_containers);

    int getNumReservedContainers();

    void setNumReservedContainers(int num_reserved_containers);

    //Resource getUsedResources();

    //void setUsedResources(Resource resources);

    //Resource getReservedResources();

    //void setReservedResources(Resource reserved_resources);

    //Resource getNeededResources();

    //void setNeededResources(Resource needed_resources);

    void setMemorySeconds(long memory_seconds);

    long getMemorySeconds();

    void setVcoreSeconds(long vcore_seconds);

    long getVcoreSeconds();

    float getQueueUsagePercentage();

    void setQueueUsagePercentage(float queueUsagePerc);

    float getClusterUsagePercentage();

    void setClusterUsagePercentage(float clusterUsagePerc);

    void setPreemptedMemorySeconds(long memorySeconds);

    long getPreemptedMemorySeconds();

    void setPreemptedVcoreSeconds(long vcoreSeconds);

    long getPreemptedVcoreSeconds();

    Map<String, Long> getResourceSecondsMap();

    void setResourceSecondsMap(Map<String, Long> resourceSecondsMap);

    Map<String, Long> getPreemptedResourceSecondsMap();

    void setPreemptedResourceSecondsMap(Map<String, Long> preemptedResourceSecondsMap);
}
