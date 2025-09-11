package org.apache.hadoop.yarn.api.protocolrecords;

public interface AllocateRequestJVMInterface {

    int getResponseId();

    java.util.List getUpdateRequests();

    void setAskList(java.util.List<org.apache.hadoop.yarn.api.records.ResourceRequest> arg0);

    void setResponseId(int arg0);

    java.util.List getAskList();

    java.util.List getReleaseList();

    float getProgress();

    org.apache.hadoop.yarn.api.records.ResourceBlacklistRequestJVMInterface getResourceBlacklistRequest();

    java.util.List getIncreaseRequests();

    void setUpdateRequests(java.util.List<org.apache.hadoop.yarn.api.records.UpdateContainerRequest> arg0);

    void setIncreaseRequests(java.util.List<org.apache.hadoop.yarn.api.records.ContainerResourceIncreaseRequest> arg0);

    void setProgress(float arg0);

    void setReleaseList(java.util.List<org.apache.hadoop.yarn.api.records.ContainerId> arg0);

    void setResourceBlacklistRequest_bridge(org.apache.hadoop.yarn.api.records.ResourceBlacklistRequestJVMInterface arg0);
}
