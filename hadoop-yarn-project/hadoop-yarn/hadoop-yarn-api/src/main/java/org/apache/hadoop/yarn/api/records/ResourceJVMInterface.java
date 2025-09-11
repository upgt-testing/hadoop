package org.apache.hadoop.yarn.api.records;

public interface ResourceJVMInterface {

    int hashCode();

    org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface getResourceInformation(java.lang.String arg0);

    int getVirtualCores();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface getResourceInformation(int arg0) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    java.lang.String getFormattedString();

    void setMemorySize(long arg0);

    int getMemory();

    void setResourceValue(java.lang.String arg0, long arg1);

    void setResourceValue(int arg0, long arg1) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    void setResourceInformation_bridge(int arg0, org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface arg1) throws org.apache.hadoop.yarn.exceptions.ResourceNotFoundException;

    long getMemorySize();

    java.util.List getAllResourcesListCopy();

    org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface[] getResources();

    long getResourceValue(java.lang.String arg0);

    void setMemory(int arg0);

    void setResourceInformation_bridge(java.lang.String arg0, org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface arg1);

    void setVirtualCores(int arg0);
}
