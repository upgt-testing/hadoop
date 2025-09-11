package org.apache.hadoop.yarn.api.records;

public interface ResourceJVMInterface {

    int hashCode();

    long getMemorySize();

    int getVirtualCores();

    boolean equals(java.lang.Object arg0);

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    java.lang.String toString();

    int getMemory();

    void setMemorySize(long arg0);

    void setMemory(int arg0);

    void setVirtualCores(int arg0);
}
