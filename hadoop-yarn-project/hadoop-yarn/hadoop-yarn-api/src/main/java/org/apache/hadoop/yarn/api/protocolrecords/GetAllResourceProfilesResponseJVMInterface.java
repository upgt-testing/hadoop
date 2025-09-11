package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetAllResourceProfilesResponseJVMInterface {

    int hashCode();

    boolean equals(java.lang.Object arg0);

    java.util.Map getResourceProfiles();

    void setResourceProfiles(java.util.Map<java.lang.String, org.apache.hadoop.yarn.api.records.Resource> arg0);
}
