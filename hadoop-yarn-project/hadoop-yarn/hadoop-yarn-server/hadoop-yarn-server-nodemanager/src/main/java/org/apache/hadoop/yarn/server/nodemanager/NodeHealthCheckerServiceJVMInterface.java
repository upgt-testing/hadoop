package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.service.CompositeServiceJVMInterface;

public interface NodeHealthCheckerServiceJVMInterface extends CompositeServiceJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.LocalDirsHandlerServiceJVMInterface getDiskHandler();
}
