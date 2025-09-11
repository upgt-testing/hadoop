package org.apache.hadoop.conf;

public interface ConfiguredJVMInterface extends ConfigurableJVMInterface {

    void setConf_bridge(org.apache.hadoop.conf.ConfigurationJVMInterface arg0);

    org.apache.hadoop.conf.ConfigurationJVMInterface getConf();
}
