package org.apache.hadoop.hdfs.protocol;

public interface DatanodeInfoJVMInterface {
    String getName();
    void setLastUpdate(long lastUpdate);
    void setLastUpdateMonotonic(long lastUpdateMonotonic);
}
