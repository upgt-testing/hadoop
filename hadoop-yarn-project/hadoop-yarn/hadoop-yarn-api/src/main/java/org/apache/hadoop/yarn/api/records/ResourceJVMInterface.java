package org.apache.hadoop.yarn.api.records;

public interface ResourceJVMInterface {

    int hashCode();

    org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface getResourceInformation(java.lang.String arg0) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    int getVirtualCores();

    boolean equals(java.lang.Object arg0);

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    java.lang.String toString();

    org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface getResourceInformation(int arg0) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    int getMemory();

    void setMemorySize(long arg0);

    void setResourceValue(java.lang.String arg0, long arg1) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    void setResourceValue(int arg0, long arg1) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    void setResourceInformation_bridge(int arg0, org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface arg1) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    long getMemorySize();

    long getResourceValue(java.lang.String arg0) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface[] getResources();

    void setMemory(int arg0);

    void setVirtualCores(int arg0);

    void setResourceInformation_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface arg1) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;
}
