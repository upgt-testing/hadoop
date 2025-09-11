package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetDelegationTokenResponseJVMInterface {

    void setRMDelegationToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getRMDelegationToken();
}
