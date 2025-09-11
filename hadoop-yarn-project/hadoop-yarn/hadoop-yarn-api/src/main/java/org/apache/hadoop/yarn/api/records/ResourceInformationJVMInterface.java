package org.apache.hadoop.yarn.api.records;

public interface ResourceInformationJVMInterface {

    int hashCode();

    long getMaximumAllocation();

    boolean equals(java.lang.Object arg0);

    void setMinimumAllocation(long arg0);

    java.lang.String toString();

    void setMaximumAllocation(long arg0);

    java.lang.String getShorthandRepresentation();

    void setValue(long arg0);

    void setUnits(java.lang.String arg0);

    void setUnitsWithoutValidation(java.lang.String arg0);

    java.lang.Object getResourceType();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface arg0);

    java.lang.String getUnits();

    java.lang.String getName();

    void setResourceType_bridge(java.lang.Object arg0);

    long getValue();

    void setName(java.lang.String arg0);

    long getMinimumAllocation();
}
