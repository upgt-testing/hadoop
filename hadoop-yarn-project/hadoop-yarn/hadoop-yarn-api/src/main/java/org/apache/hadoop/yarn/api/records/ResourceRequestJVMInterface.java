package org.apache.hadoop.yarn.api.records;

public interface ResourceRequestJVMInterface {

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setAllocationRequestId(long arg0);

    int hashCode();

    long getAllocationRequestId();

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getCapability();

    boolean equals(java.lang.Object arg0);

    void setExecutionTypeRequest_bridge(org.apache.hadoop.yarn.api.records.ExecutionTypeRequestJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ExecutionTypeRequestJVMInterface getExecutionTypeRequest();

    void setNumContainers(int arg0);

    int getNumContainers();

    void setResourceName(java.lang.String arg0);

    java.lang.String getResourceName();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface arg0);

    void setNodeLabelExpression(java.lang.String arg0);

    boolean getRelaxLocality();

    void setRelaxLocality(boolean arg0);

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    java.lang.String getNodeLabelExpression();

    void setCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);
}
