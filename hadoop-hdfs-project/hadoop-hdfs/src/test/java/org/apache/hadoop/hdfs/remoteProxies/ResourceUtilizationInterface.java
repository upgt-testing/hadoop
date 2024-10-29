package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

public interface ResourceUtilizationInterface {

    int getVirtualMemory();

    void setVirtualMemory(int vmem);

    int getPhysicalMemory();

    void setPhysicalMemory(int pmem);

    float getCPU();

    void setCPU(float cpu);

    float getCustomResource(String resourceName);

    Map<String, Float> getCustomResources();

    void setCustomResources(Map<String, Float> customResources);

    void setCustomResource(String resourceName, float utilization);

    int hashCode();

    boolean equals(Object obj);

    String toString();

    void addTo(int pmem, int vmem, float cpu);

    void addTo(int pmem, int vmem, float cpu, String resourceName, float utilization);

    void subtractFrom(int pmem, int vmem, float cpu);

    void subtractFrom(int pmem, int vmem, float cpu, String resourceName, float utilization);
}
