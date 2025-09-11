package org.apache.hadoop.yarn.api.records;

public interface PreemptionMessageJVMInterface {

    org.apache.hadoop.yarn.api.records.PreemptionContractJVMInterface getContract();

    void setContract_bridge(org.apache.hadoop.yarn.api.records.PreemptionContractJVMInterface arg0);

    void setStrictContract_bridge(org.apache.hadoop.yarn.api.records.StrictPreemptionContractJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.StrictPreemptionContractJVMInterface getStrictContract();
}
