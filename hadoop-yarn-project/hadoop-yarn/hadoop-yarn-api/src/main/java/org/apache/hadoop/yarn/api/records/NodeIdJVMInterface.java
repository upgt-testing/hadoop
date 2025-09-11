package org.apache.hadoop.yarn.api.records;

public interface NodeIdJVMInterface {

    int hashCode();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.NodeIdJVMInterface arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    int getPort();

    java.lang.String getHost();
}
