package org.apache.hadoop.yarn.api.records;

public interface QueueInfoJVMInterface {

    java.lang.String getDefaultNodeLabelExpression();

    void setMaximumCapacity(float arg0);

    float getCurrentCapacity();

    void setDefaultNodeLabelExpression(java.lang.String arg0);

    java.lang.Object getQueueState();

    float getMaximumCapacity();

    java.lang.String getQueueName();

    float getCapacity();

    java.lang.Boolean getPreemptionDisabled();

    void setQueueState_bridge(java.lang.Object arg0);

    void setIntraQueuePreemptionDisabled(boolean arg0);

    void setPreemptionDisabled(boolean arg0);

    void setAccessibleNodeLabels(java.util.Set<java.lang.String> arg0);

    java.lang.Boolean getIntraQueuePreemptionDisabled();

    void setQueueStatistics_bridge(org.apache.hadoop.yarn.api.records.QueueStatisticsJVMInterface arg0);

    java.util.Set getAccessibleNodeLabels();

    org.apache.hadoop.yarn.api.records.QueueStatisticsJVMInterface getQueueStatistics();

    void setApplications(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationReport> arg0);

    void setQueueName(java.lang.String arg0);

    java.util.List getChildQueues();

    void setChildQueues(java.util.List<org.apache.hadoop.yarn.api.records.QueueInfo> arg0);

    java.util.List getApplications();

    void setCapacity(float arg0);

    void setCurrentCapacity(float arg0);
}
