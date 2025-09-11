package org.apache.hadoop.yarn.api.protocolrecords;

public interface RegisterApplicationMasterResponseJVMInterface {

    java.util.EnumSet getSchedulerResourceTypes();

    void setNMTokensFromPreviousAttempts(java.util.List<org.apache.hadoop.yarn.api.records.NMToken> arg0);

    void setQueue(java.lang.String arg0);

    void setMaximumResourceCapability_bridge(org.apache.hadoop.yarn.api.records.ResourceJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ResourceJVMInterface getMaximumResourceCapability();

    void setApplicationACLs(java.util.Map<org.apache.hadoop.yarn.api.records.ApplicationAccessType, java.lang.String> arg0);

    java.lang.String getQueue();

    java.util.List getNMTokensFromPreviousAttempts();

    java.nio.ByteBuffer getClientToAMTokenMasterKey();

    void setClientToAMTokenMasterKey(java.nio.ByteBuffer arg0);

    void setSchedulerResourceTypes(java.util.EnumSet<org.apache.hadoop.yarn.proto.YarnServiceProtos.SchedulerResourceTypes> arg0);

    java.util.List getContainersFromPreviousAttempts();

    java.util.Map getApplicationACLs();

    void setContainersFromPreviousAttempts(java.util.List<org.apache.hadoop.yarn.api.records.Container> arg0);
}
