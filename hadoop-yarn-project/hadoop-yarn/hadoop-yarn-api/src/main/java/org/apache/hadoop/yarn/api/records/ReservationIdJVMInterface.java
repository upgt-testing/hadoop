package org.apache.hadoop.yarn.api.records;

public interface ReservationIdJVMInterface {

    int hashCode();

    long getClusterTimestamp();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    long getId();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ReservationIdJVMInterface arg0);
}
