package org.apache.hadoop.yarn.api.records;

public interface ResourceRequestJVMInterface {

    void setAllocationRequestId(long arg0);

    long getAllocationRequestId();

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    int hashCode();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getCapability();

    boolean equals(java.lang.Object arg0);

    void setExecutionTypeRequest_bridge(org.apache.hadoop.yarn.api.records.ExecutionTypeRequestJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ExecutionTypeRequestJVMInterface getExecutionTypeRequest();

    void setNumContainers(int arg0);

    int getNumContainers();

    void setResourceName(java.lang.String arg0);

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface arg0);

    java.lang.String getResourceName();

    void setNodeLabelExpression(java.lang.String arg0);

    void setRelaxLocality(boolean arg0);

    boolean getRelaxLocality();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    java.lang.String getNodeLabelExpression();

    void setCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);
}
