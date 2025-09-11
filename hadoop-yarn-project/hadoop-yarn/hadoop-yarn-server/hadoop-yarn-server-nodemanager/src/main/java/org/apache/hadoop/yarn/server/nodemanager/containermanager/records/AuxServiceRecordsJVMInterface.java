package org.apache.hadoop.yarn.server.nodemanager.containermanager.records;

public interface AuxServiceRecordsJVMInterface {

    java.util.List getServices();

    org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordsJVMInterface serviceList_bridge(org.apache.hadoop.yarn.server.nodemanager.containermanager.records.AuxServiceRecordJVMInterface[] arg0);
}
