package org.apache.hadoop.security.authorize;

public interface PolicyProviderJVMInterface {

    org.apache.hadoop.security.authorize.ServiceJVMInterface[] getServices();
}
