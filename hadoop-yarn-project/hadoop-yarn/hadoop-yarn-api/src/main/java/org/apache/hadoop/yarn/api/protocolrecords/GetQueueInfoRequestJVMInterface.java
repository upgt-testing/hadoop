package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetQueueInfoRequestJVMInterface {

    boolean getIncludeChildQueues();

    void setIncludeApplications(boolean arg0);

    boolean getIncludeApplications();

    boolean getRecursive();

    java.lang.String getQueueName();

    void setIncludeChildQueues(boolean arg0);

    void setRecursive(boolean arg0);

    void setQueueName(java.lang.String arg0);
}
