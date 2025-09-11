package org.apache.hadoop.yarn.api.records;

public interface ResourceUtilizationJVMInterface {

    int getPhysicalMemory();

    int hashCode();

    void addTo(int arg0, int arg1, float arg2);

    boolean equals(java.lang.Object arg0);

    int getVirtualMemory();

    java.lang.String toString();

    void setVirtualMemory(int arg0);

    void setPhysicalMemory(int arg0);

    void subtractFrom(int arg0, int arg1, float arg2);

    float getCPU();

    void setCPU(float arg0);
}
