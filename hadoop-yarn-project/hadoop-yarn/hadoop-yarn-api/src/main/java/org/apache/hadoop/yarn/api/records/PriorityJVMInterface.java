package org.apache.hadoop.yarn.api.records;

public interface PriorityJVMInterface {

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.PriorityJVMInterface arg0);

    void setPriority(int arg0);

    int hashCode();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    int getPriority();
}
