package org.apache.hadoop.yarn.server.nodemanager.containermanager.deletion.task;

public interface DeletionTaskJVMInterface {

    int getTaskId();

    void setTaskId(int arg0);

    org.apache.hadoop.yarn.server.nodemanager.DeletionServiceJVMInterface getDeletionService();

    java.lang.String getUser();

    int incrementAndGetPendingPredecessorTasks();

    java.lang.Object convertDeletionTaskToProto();

    org.apache.hadoop.yarn.server.nodemanager.containermanager.deletion.task.DeletionTaskJVMInterface[] getSuccessorTasks();

    void setSuccess(boolean arg0);

    boolean getSucess();

    java.lang.Object getDeletionTaskType();

    int decrementAndGetPendingPredecessorTasks();

    void addDeletionTaskDependency_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.deletion.task.DeletionTaskJVMInterface arg0);
}
