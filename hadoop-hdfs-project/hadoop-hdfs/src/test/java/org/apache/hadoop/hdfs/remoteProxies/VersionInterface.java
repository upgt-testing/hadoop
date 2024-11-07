package org.apache.hadoop.hdfs.remoteProxies;

public interface VersionInterface {
    boolean equals(java.lang.Object arg0);
    java.lang.String toFullString();
    java.lang.String getArtifactId();
    java.lang.String toString();
    boolean isSnapshot();
    int getPatchLevel();
    boolean isUnknownVersion();
    int compareTo(VersionInterface arg0);
    VersionInterface unknownVersion();
    java.lang.String getGroupId();
    int getMajorVersion();
    int getMinorVersion();
    boolean isUknownVersion();
    int hashCode();
}