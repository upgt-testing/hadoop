package org.apache.hadoop.ipc;

import org.apache.hadoop.conf.ConfigurationJVMInterface;

public interface RPCServerJVMInterface extends ServerJVMInterface {
    void refreshCallQueue(ConfigurationJVMInterface conf);
}
