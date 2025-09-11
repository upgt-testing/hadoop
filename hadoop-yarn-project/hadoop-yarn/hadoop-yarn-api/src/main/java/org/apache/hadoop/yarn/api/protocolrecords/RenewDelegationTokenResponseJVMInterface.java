package org.apache.hadoop.yarn.api.protocolrecords;

public interface RenewDelegationTokenResponseJVMInterface {

    long getNextExpirationTime();

    void setNextExpirationTime(long arg0);
}
