package org.apache.hadoop.yarn.api.records;

public interface ApplicationAttemptIdJVMInterface {

    int hashCode();

    int compareTo_bridge(org.apache.hadoop.yarn.api.records.ApplicationAttemptIdJVMInterface arg0);

    org.apache.hadoop.yarn.api.records.ApplicationIdJVMInterface getApplicationId();

    int getAttemptId();

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();
}
