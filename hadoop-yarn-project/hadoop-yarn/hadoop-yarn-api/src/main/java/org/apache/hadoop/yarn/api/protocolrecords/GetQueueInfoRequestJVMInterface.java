package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetQueueInfoRequestJVMInterface {

    boolean getIncludeChildQueues();

    void setIncludeApplications(boolean arg0);

    boolean getIncludeApplications();

    boolean getRecursive();

    void setIncludeChildQueues(boolean arg0);

    java.lang.String getQueueName();

    void setRecursive(boolean arg0);

    void setQueueName(java.lang.String arg0);
}
