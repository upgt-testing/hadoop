package org.apache.hadoop.yarn.api.records;

public interface ContainerRetryContextJVMInterface {

    void setErrorCodes(java.util.Set<java.lang.Integer> arg0);

    int getRetryInterval();

    void setRetryInterval(int arg0);

    long getFailuresValidityInterval();

    int getMaxRetries();

    java.lang.Object getRetryPolicy();

    java.util.Set getErrorCodes();

    void setFailuresValidityInterval(long arg0);

    void setRetryPolicy_bridge(java.lang.Object arg0);

    void setMaxRetries(int arg0);
}
