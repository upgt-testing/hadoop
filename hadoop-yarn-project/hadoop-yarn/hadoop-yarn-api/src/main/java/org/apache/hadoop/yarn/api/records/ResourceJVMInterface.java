package org.apache.hadoop.yarn.api.records;

public interface ResourceJVMInterface {

    int hashCode();

    int getVirtualCores();

    long getMemorySize();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    int getMemory();

    void setMemorySize(long arg0);

    void setMemory(int arg0);

    void setVirtualCores(int arg0);
}
