package org.apache.hadoop.yarn.api.protocolrecords;

public interface CancelDelegationTokenRequestJVMInterface {

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getDelegationToken();

    void setDelegationToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);
}
