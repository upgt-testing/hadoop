package org.apache.hadoop.hdfs.remoteProxies;

public interface MapFieldInterface<K, V> {
    void convertMessageToKeyAndValue(org.apache.hadoop.thirdparty.protobuf.Message arg0, java.util.Map<K, V> arg1);
    MapFieldInterface<K, V> copy();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Message> getMutableList();
    void ensureMutable();
    org.apache.hadoop.thirdparty.protobuf.Message convertKeyAndValueToMessage(K arg0, V arg1);
    void clear();
    java.util.Map<K, V> getMutableMap();
    <K, V> MapFieldInterface<K, V> emptyMapField(MapEntryInterface<K, V> arg0);
    java.util.Map<K, V> getMap();
    void makeImmutable();
    boolean isMutable();
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Message> convertMapToList(MutatabilityAwareMapInterface<K, V> arg0);
    boolean equals(java.lang.Object arg0);
    <K, V> MapFieldInterface<K, V> newMapField(MapEntryInterface<K, V> arg0);
    java.util.List<org.apache.hadoop.thirdparty.protobuf.Message> getList();
    void mergeFrom(MapFieldInterface<K, V> arg0);
    org.apache.hadoop.thirdparty.protobuf.Message getMapEntryMessageDefaultInstance();
    MutatabilityAwareMapInterface<K, V> convertListToMap(java.util.List<org.apache.hadoop.thirdparty.protobuf.Message> arg0);
    int hashCode();
}