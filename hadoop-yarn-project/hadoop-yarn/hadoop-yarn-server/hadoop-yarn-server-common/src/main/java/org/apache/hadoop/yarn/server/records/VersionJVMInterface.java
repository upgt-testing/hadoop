package org.apache.hadoop.yarn.server.records;

public interface VersionJVMInterface {

    int hashCode();

    void setMajorVersion(int arg0);

    boolean equals(java.lang.Object arg0);

    java.lang.String toString();

    void setMinorVersion(int arg0);

    boolean isCompatibleTo_bridge(org.apache.hadoop.yarn.server.records.VersionJVMInterface arg0);

    int getMajorVersion();

    int getMinorVersion();
}
