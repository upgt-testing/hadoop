package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetQueueInfoResponseJVMInterface {

    org.apache.hadoop.yarn.api.records.QueueInfoJVMInterface getQueueInfo();

    void setQueueInfo_bridge(org.apache.hadoop.yarn.api.records.QueueInfoJVMInterface arg0);
}
