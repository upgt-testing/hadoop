package org.apache.hadoop.yarn.api.records;

public interface LogAggregationContextJVMInterface {

    void setExcludePattern(java.lang.String arg0);

    java.lang.String getLogAggregationPolicyClassName();

    java.lang.String getRolledLogsExcludePattern();

    java.lang.String getRolledLogsIncludePattern();

    void setIncludePattern(java.lang.String arg0);

    java.lang.String getLogAggregationPolicyParameters();

    void setRolledLogsExcludePattern(java.lang.String arg0);

    java.lang.String getIncludePattern();

    void setRolledLogsIncludePattern(java.lang.String arg0);

    java.lang.String getExcludePattern();

    void setLogAggregationPolicyClassName(java.lang.String arg0);

    void setLogAggregationPolicyParameters(java.lang.String arg0);
}
