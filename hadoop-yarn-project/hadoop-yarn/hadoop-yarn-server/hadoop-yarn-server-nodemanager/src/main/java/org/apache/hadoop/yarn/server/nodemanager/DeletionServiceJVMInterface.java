package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface DeletionServiceJVMInterface extends AbstractServiceJVMInterface {

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getStateStore();

    int getDebugDelay();

    void serviceStop() throws java.lang.Exception;

    org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface getContainerExecutor();

    boolean isTerminated();

    void delete_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.deletion.task.DeletionTaskJVMInterface arg0);
}
