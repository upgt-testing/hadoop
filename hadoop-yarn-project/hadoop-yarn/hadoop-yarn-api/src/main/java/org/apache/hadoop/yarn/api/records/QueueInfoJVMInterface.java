package org.apache.hadoop.yarn.api.records;

public interface QueueInfoJVMInterface {

    void setDefaultNodeLabelExpression(java.lang.String arg0);

    java.lang.Object getQueueState();

    float getCapacity();

    java.lang.Boolean getPreemptionDisabled();

    void setQueueConfigurations(java.util.Map<java.lang.String, org.apache.hadoop.yarn.api.records.QueueConfigurations> arg0);

    void setAccessibleNodeLabels(java.util.Set<java.lang.String> arg0);

    void setApplications(java.util.List<org.apache.hadoop.yarn.api.records.ApplicationReport> arg0);

    java.util.List getApplications();

    java.util.Map getQueueConfigurations();

    java.lang.String getDefaultNodeLabelExpression();

    void setMaximumCapacity(float arg0);

    float getCurrentCapacity();

    float getMaximumCapacity();

    java.lang.String getQueueName();

    void setQueueState_bridge(java.lang.Object arg0);

    void setIntraQueuePreemptionDisabled(boolean arg0);

    void setPreemptionDisabled(boolean arg0);

    java.lang.Boolean getIntraQueuePreemptionDisabled();

    void setQueueStatistics_bridge(org.apache.hadoop.yarn.api.records.QueueStatisticsJVMInterface arg0);

    java.util.Set getAccessibleNodeLabels();

    org.apache.hadoop.yarn.api.records.QueueStatisticsJVMInterface getQueueStatistics();

    void setQueueName(java.lang.String arg0);

    java.util.List getChildQueues();

    void setCapacity(float arg0);

    void setChildQueues(java.util.List<org.apache.hadoop.yarn.api.records.QueueInfo> arg0);

    void setCurrentCapacity(float arg0);
}
