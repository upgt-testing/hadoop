package org.apache.hadoop.yarn.api.protocolrecords;

public interface AllocateRequestJVMInterface {

    int getResponseId();

    java.util.List getUpdateRequests();

    java.lang.String getTrackingUrl();

    void setAskList(java.util.List<org.apache.hadoop.yarn.api.records.ResourceRequest> arg0);

    void setResponseId(int arg0);

    java.util.List getAskList();

    float getProgress();

    java.util.List getReleaseList();

    void setSchedulingRequests(java.util.List<org.apache.hadoop.yarn.api.records.SchedulingRequest> arg0);

    org.apache.hadoop.yarn.api.records.ResourceBlacklistRequestJVMInterface getResourceBlacklistRequest();

    void setUpdateRequests(java.util.List<org.apache.hadoop.yarn.api.records.UpdateContainerRequest> arg0);

    void setProgress(float arg0);

    void setReleaseList(java.util.List<org.apache.hadoop.yarn.api.records.ContainerId> arg0);

    void setResourceBlacklistRequest_bridge(org.apache.hadoop.yarn.api.records.ResourceBlacklistRequestJVMInterface arg0);

    java.util.List getSchedulingRequests();

    void setTrackingUrl(java.lang.String arg0);
}
