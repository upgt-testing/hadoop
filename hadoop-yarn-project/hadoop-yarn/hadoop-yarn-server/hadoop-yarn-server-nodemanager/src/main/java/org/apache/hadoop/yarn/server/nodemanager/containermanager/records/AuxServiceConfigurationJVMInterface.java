package org.apache.hadoop.yarn.server.nodemanager.containermanager.records;

public interface AuxServiceConfigurationJVMInterface {

    int hashCode();

    java.util.Map getProperties();

    java.lang.String getProperty(java.lang.String arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    java.lang.String getProperty(java.lang.String arg0, java.lang.String arg1);

    void setProperties(java.util.Map<java.lang.String, java.lang.String> arg0);

    void setFiles(java.util.List<org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceFile> arg0);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceConfigurationJVMInterface properties(java.util.Map<java.lang.String, java.lang.String> arg0);

    java.util.List getFiles();

    void setProperty(java.lang.String arg0, java.lang.String arg1);

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceConfigurationJVMInterface files(java.util.List<org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceFile> arg0);
}
