package org.apache.hadoop.yarn.api.records;

public interface ResourceUtilizationJVMInterface {

    int hashCode();

    int getPhysicalMemory();

    void addTo(int arg0, int arg1, float arg2);

    boolean equals(java.lang.Object arg0);

    int getVirtualMemory();

    void setVirtualMemory(int arg0);

    java.lang.String toString();

    void setPhysicalMemory(int arg0);

    float getCPU();

    void subtractFrom(int arg0, int arg1, float arg2);

    void setCPU(float arg0);
}
