package org.apache.hadoop.yarn.api.protocolrecords;

public interface GetApplicationsRequestJVMInterface {

    long getLimit();

    org.apache.commons.lang.math.LongRange getFinishRange();

    java.util.Set getApplicationTypes();

    void setFinishRange(org.apache.commons.lang.math.LongRange arg0);

    void setLimit(long arg0);

    void setApplicationTypes(java.util.Set<java.lang.String> arg0);

    void setApplicationTags(java.util.Set<java.lang.String> arg0);

    org.apache.commons.lang.math.LongRange getStartRange();

    void setFinishRange(long arg0, long arg1);

    java.util.Set getApplicationTags();

    java.util.Set getQueues();

    java.util.Set getUsers();

    void setStartRange(long arg0, long arg1) throws java.lang.IllegalArgumentException;

    void setApplicationStates(java.util.Set<java.lang.String> arg0);

    void setUsers(java.util.Set<java.lang.String> arg0);

    void setScope_bridge(java.lang.Object arg0);

    void setStartRange(org.apache.commons.lang.math.LongRange arg0);

    void setQueues(java.util.Set<java.lang.String> arg0);

    void setApplicationStates(java.util.EnumSet<org.apache.hadoop.yarn.api.records.YarnApplicationState> arg0);

    java.util.EnumSet getApplicationStates();

    java.lang.Object getScope();
}
