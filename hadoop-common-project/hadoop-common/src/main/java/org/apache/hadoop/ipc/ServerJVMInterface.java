package org.apache.hadoop.ipc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.ConfigurationJVMInterface;

import java.io.IOException;

public interface ServerJVMInterface {
    int getMaxQueueSize();
    //void refreshCallQueue(ConfigurationJVMInterface conf) throws IOException; // TODO: This might need to be changed to ConfigurationJVMInterface
}
