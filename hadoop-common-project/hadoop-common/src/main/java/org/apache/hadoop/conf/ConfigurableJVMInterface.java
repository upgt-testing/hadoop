package org.apache.hadoop.conf;

public interface ConfigurableJVMInterface {

    void setConf_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    org.apache.hadoop.conf.ConfigurationJVMInterface getConf();
}
