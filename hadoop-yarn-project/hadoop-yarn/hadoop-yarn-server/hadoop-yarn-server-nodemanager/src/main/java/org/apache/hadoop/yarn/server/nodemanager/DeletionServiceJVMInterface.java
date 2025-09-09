package org.apache.hadoop.yarn.server.nodemanager;

import org.apache.hadoop.service.AbstractServiceJVMInterface;

public interface DeletionServiceJVMInterface extends AbstractServiceJVMInterface {

    int getDebugDelay();

    org.apache.hadoop.yarn.server.nodemanager.recovery.NMStateStoreServiceJVMInterface getStateStore();

    void serviceStop() throws java.lang.Exception;

    org.apache.hadoop.yarn.server.nodemanager.ContainerExecutorJVMInterface getContainerExecutor();

    boolean isTerminated();

    void delete_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.deletion.task.DeletionTaskJVMInterface arg0);
}
