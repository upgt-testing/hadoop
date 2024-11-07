package org.apache.hadoop.hdfs.remoteProxies;

public interface SlotIdInterface {
    java.lang.String toString();
    boolean equals(java.lang.Object arg0);
    ShmIdInterface getShmId();
    int hashCode();
    int getSlotIdx();
}