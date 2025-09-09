package org.apache.hadoop.yarn.api.records;

public interface ResourceUtilizationJVMInterface {

    int hashCode();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    void setCustomResource(java.lang.String arg0, float arg1);

    java.util.Map getCustomResources();

    void setPhysicalMemory(int arg0);

    void subtractFrom(int arg0, int arg1, float arg2);

    float getCPU();

    void setCPU(float arg0);

    float getCustomResource(java.lang.String arg0);

    int getPhysicalMemory();

    void addTo(int arg0, int arg1, float arg2);

    int getVirtualMemory();

    void setVirtualMemory(int arg0);

    void addTo(int arg0, int arg1, float arg2, java.lang.String arg3, float arg4);

    void setCustomResources(java.util.Map<java.lang.String, java.lang.Float> arg0);

    void subtractFrom(int arg0, int arg1, float arg2, java.lang.String arg3, float arg4);
}
