package org.apache.hadoop.yarn.api.records;

public interface ResourceRequestJVMInterface {

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    long getAllocationRequestId();

    void setAllocationRequestId(long arg0);

    int hashCode();

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getCapability();

    void setExecutionTypeRequest_bridge(org.apache.hadoop.yarn.api.records.ExecutionTypeRequestJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ExecutionTypeRequestJVMInterface getExecutionTypeRequest();

    void setNumContainers(int arg0);

    int getNumContainers();

    void setResourceName(java.lang.String arg0);

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface arg0);

    java.lang.String getResourceName();

    void setNodeLabelExpression(java.lang.String arg0);

    void setRelaxLocality(boolean arg0);

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    boolean getRelaxLocality();

    void setCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    java.lang.String getNodeLabelExpression();
}
