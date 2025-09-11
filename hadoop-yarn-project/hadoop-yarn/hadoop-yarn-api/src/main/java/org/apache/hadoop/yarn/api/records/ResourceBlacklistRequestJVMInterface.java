package org.apache.hadoop.yarn.api.records;

public interface ResourceBlacklistRequestJVMInterface {

    void setBlacklistAdditions(java.util.List<java.lang.String> arg0);

    java.util.List getBlacklistRemovals();

    java.util.List getBlacklistAdditions();

    void setBlacklistRemovals(java.util.List<java.lang.String> arg0);
}
