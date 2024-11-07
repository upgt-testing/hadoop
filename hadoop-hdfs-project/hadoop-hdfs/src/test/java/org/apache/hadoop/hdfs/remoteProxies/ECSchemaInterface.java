package org.apache.hadoop.hdfs.remoteProxies;

public interface ECSchemaInterface {
    int getNumDataUnits();
    int extractIntOption(java.lang.String arg0, java.util.Map<java.lang.String, java.lang.String> arg1);
    java.lang.String getCodecName();
    int hashCode();
    boolean equals(java.lang.Object arg0);
    java.util.Map<java.lang.String, java.lang.String> getExtraOptions();
    java.lang.String toString();
    int getNumParityUnits();
}