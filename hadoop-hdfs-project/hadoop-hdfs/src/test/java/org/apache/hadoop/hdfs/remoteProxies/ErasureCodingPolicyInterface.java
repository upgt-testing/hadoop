package org.apache.hadoop.hdfs.remoteProxies;

public interface ErasureCodingPolicyInterface {
    java.lang.String getName();
    int getCellSize();
    boolean equals(java.lang.Object arg0);
    int getNumDataUnits();
    byte getId();
    java.lang.String composePolicyName(ECSchemaInterface arg0, int arg1);
    int hashCode();
    ECSchemaInterface getSchema();
    boolean isSystemPolicy();
    boolean isReplicationPolicy();
    int getNumParityUnits();
    java.lang.String toString();
    java.lang.String getCodecName();
}