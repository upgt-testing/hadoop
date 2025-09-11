package org.apache.hadoop.yarn.api.records;

public interface ApplicationIdJVMInterface {

    int hashCode();

    long getClusterTimestamp();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    int getId();
}
