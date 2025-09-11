package org.apache.hadoop.yarn.api.records;

public interface PriorityJVMInterface {

    int hashCode();

    void setPriority(int arg0);

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    int getPriority();
}
