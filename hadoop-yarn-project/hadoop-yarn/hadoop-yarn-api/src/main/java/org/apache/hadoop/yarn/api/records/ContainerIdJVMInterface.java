package org.apache.hadoop.yarn.api.records;

public interface ContainerIdJVMInterface {

    int hashCode();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ContainerIdJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    long getContainerId();

    int getId();

    org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface getApplicationAttemptId();
}
