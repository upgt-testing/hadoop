package org.apache.hadoop.yarn.api.records;

public interface ResourceInformationJVMInterface {

    int hashCode();

    long getMaximumAllocation();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    void setMinimumAllocation(long arg0);

    void setMaximumAllocation(long arg0);

    java.lang.String getShorthandRepresentation();

    void setUnits(java.lang.String arg0);

    void setValue(long arg0);

    void setUnitsWithoutValidation(java.lang.String arg0);

    java.lang.Object getResourceType();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceInformationJVMInterface arg0);

    java.lang.String getUnits();

    java.lang.String getName();

    long getValue();

    void setResourceType_bridge(java.lang.Object arg0);

    long getMinimumAllocation();

    void setName(java.lang.String arg0);
}
