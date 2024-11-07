package org.apache.hadoop.hdfs.remoteProxies;

public interface NumberReplicasInterface {
    int decommissionedAndDecommissioning();
    void reset();
    int corruptReplicas();
    int replicasOnStaleNodes();
    boolean equals(java.lang.Object arg0);
    long sum();
    int redundantInternalBlocks();
//    void subtract(E arg0, long arg1);
    int maintenanceReplicas();
    void negation();
//    void subtract(EnumCountersInterface<E> arg0);
    java.lang.String toString();
    void reset(long arg0);
    int decommissioning();
    long[] asArray();
    int excessReplicas();
    int outOfServiceReplicas();
//    EnumCountersInterface<E> deepCopyEnumCounter();
    int readOnlyReplicas();
//    void add(E arg0, long arg1);
//    void set(E arg0, long arg1);
    int decommissioned();
    int hashCode();
    boolean allLessOrEqual(long arg0);
    int liveReplicas();
//    void add(EnumCountersInterface<E> arg0);
//    long get(E arg0);
    int liveEnteringMaintenanceReplicas();
    int maintenanceNotForReadReplicas();
    boolean anyGreaterOrEqual(long arg0);
//    void set(EnumCountersInterface<E> arg0);
}