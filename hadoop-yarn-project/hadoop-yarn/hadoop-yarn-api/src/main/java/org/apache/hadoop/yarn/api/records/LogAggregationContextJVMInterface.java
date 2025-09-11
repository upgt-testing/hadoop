package org.apache.hadoop.yarn.api.records;

public interface LogAggregationContextJVMInterface {

    void setExcludePattern(java.lang.String arg0);

    java.lang.String getLogAggregationPolicyClassName();

    java.lang.String getRolledLogsExcludePattern();

    void setIncludePattern(java.lang.String arg0);

    java.lang.String getRolledLogsIncludePattern();

    java.lang.String getLogAggregationPolicyParameters();

    java.lang.String getIncludePattern();

    void setRolledLogsExcludePattern(java.lang.String arg0);

    void setRolledLogsIncludePattern(java.lang.String arg0);

    java.lang.String getExcludePattern();

    void setLogAggregationPolicyClassName(java.lang.String arg0);

    void setLogAggregationPolicyParameters(java.lang.String arg0);
}
