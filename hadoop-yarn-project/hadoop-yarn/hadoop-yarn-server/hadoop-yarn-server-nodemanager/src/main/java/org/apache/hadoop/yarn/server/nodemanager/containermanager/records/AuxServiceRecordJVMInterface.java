package org.apache.hadoop.yarn.server.nodemanager.containermanager.records;

public interface AuxServiceRecordJVMInterface {

    int hashCode();

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface name(java.lang.String arg0);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface version(java.lang.String arg0);

    void setLaunchTime(java.util.Date arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.lang.String getVersion();

    void setConfiguration_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceConfigurationJVMInterface arg0);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface configuration_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceConfigurationJVMInterface arg0);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceConfigurationJVMInterface getConfiguration();

    void setVersion(java.lang.String arg0);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface description(java.lang.String arg0);

    java.lang.String getDescription();

    java.util.Date getLaunchTime();

    java.lang.String getName();

    void setName(java.lang.String arg0);

    void setDescription(java.lang.String arg0);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface launchTime(java.util.Date arg0);
}
