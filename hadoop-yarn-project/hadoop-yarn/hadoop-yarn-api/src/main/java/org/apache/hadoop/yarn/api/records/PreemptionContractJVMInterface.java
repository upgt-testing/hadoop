package org.apache.hadoop.yarn.api.records;

public interface PreemptionContractJVMInterface {

    void setContainers(java.util.Set<org.apache.hadoop.yarn.api.records.PreemptionContainer> arg0);

    java.util.Set getContainers();

    void setResourceRequest(java.util.List<org.apache.hadoop.yarn.api.records.PreemptionResourceRequest> arg0);

    java.util.List getResourceRequest();
}
