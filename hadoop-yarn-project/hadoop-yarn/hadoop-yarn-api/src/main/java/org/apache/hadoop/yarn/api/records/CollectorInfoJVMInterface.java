package org.apache.hadoop.yarn.api.records;

public interface CollectorInfoJVMInterface {

    org.apache.hadoop.yarn.api.records.TokenJVMInterface getCollectorToken();

    java.lang.String getCollectorAddr();

    void setCollectorToken_bridge(org.apache.hadoop.yarn.api.records.TokenJVMInterface arg0);

    void setCollectorAddr(java.lang.String arg0);
}
