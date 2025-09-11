package org.apache.hadoop.yarn.server.nodemanager.webapp;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface WebServerJVMInterface extends AbstractServiceJVMInterface {

    int getPort();
}
