package org.apache.hadoop.yarn.api.records;

public interface LogAggregationContextJVMInterface {

    void setExcludePattern(java.lang.String arg0);

    java.lang.String getLogAggregationPolicyClassName();

    java.lang.String getRolledLogsExcludePattern();

    java.lang.String getRolledLogsIncludePattern();

    void setIncludePattern(java.lang.String arg0);

    java.lang.String getLogAggregationPolicyParameters();

    java.lang.String getIncludePattern();

    void setRolledLogsExcludePattern(java.lang.String arg0);

    void setRolledLogsIncludePattern(java.lang.String arg0);

    java.lang.String getExcludePattern();

    void setLogAggregationPolicyParameters(java.lang.String arg0);

    void setLogAggregationPolicyClassName(java.lang.String arg0);
}
