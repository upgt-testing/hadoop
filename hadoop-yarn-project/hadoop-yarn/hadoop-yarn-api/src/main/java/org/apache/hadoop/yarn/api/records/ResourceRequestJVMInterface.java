package org.apache.hadoop.yarn.api.records;

public interface ResourceRequestJVMInterface {

    int hashCode();

    void setPriority_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getCapability();

    void setNumContainers(int arg0);

    int getNumContainers();

    void setResourceName(java.lang.String arg0);

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ResourceRequestJVMInterface arg0);

    java.lang.String getResourceName();

    void setNodeLabelExpression(java.lang.String arg0);

    boolean getRelaxLocality();

    org.apache.hadoop.yarn.api.records.PriorityJVMInterface getPriority();

    void setRelaxLocality(boolean arg0);

    void setCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    java.lang.String getNodeLabelExpression();
}
