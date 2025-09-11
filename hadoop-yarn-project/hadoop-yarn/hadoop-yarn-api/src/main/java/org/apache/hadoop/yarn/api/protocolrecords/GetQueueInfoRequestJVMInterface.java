package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetQueueInfoRequestJVMInterface {

    boolean getIncludeChildQueues();

    void setIncludeApplications(boolean arg0);

    boolean getIncludeApplications();

    boolean getRecursive();

    void setRecursive(boolean arg0);

    void setIncludeChildQueues(boolean arg0);

    java.lang.String getQueueName();

    void setQueueName(java.lang.String arg0);
}
