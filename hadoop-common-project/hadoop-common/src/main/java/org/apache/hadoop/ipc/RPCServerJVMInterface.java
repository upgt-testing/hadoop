package org.apache.hadoop.ipc;

import org.apache.hadoop.conf.ConfigurationJVMInterface;

import java.net.InetSocketAddress;

public interface RPCServerJVMInterface extends ServerJVMInterface {
    InetSocketAddress getListenerAddress();
    void refreshCallQueue(ConfigurationJVMInterface conf);
}
