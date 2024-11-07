package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtensionRegistryInterface {
    ExtensionInfoInterface findImmutableExtensionByNumber(DescriptorInterface arg0, int arg1);
    void add(GeneratedExtensionInterface<?, ?> arg0);
    void setEagerlyParseMessageSets(boolean arg0);
    void add(FieldDescriptorInterface arg0, org.apache.hadoop.thirdparty.protobuf.Message arg1);
    ExtensionInfoInterface findMutableExtensionByNumber(DescriptorInterface arg0, int arg1);
    ExtensionInfoInterface findMutableExtensionByName(java.lang.String arg0);
    ExtensionInfoInterface findImmutableExtensionByName(java.lang.String arg0);
    ExtensionRegistryLiteInterface getEmptyRegistry();
//    ExtensionRegistryInterface getEmptyRegistry();
    ExtensionRegistryInterface getUnmodifiable();
    ExtensionRegistryInterface newInstance();
    <ContainingType> GeneratedExtensionInterface<ContainingType, ?> findLiteExtensionByNumber(ContainingType arg0, int arg1);
    void add(ExtensionLiteInterface<?, ?> arg0);
    void add(ExtensionInterface<?, ?> arg0);
//    void add(ExtensionInfoInterface arg0, org.apache.hadoop.thirdparty.protobuf.Extension.ExtensionType arg1);
    java.util.Set<org.apache.hadoop.thirdparty.protobuf.ExtensionRegistry.ExtensionInfo> getAllImmutableExtensionsByExtendedType(java.lang.String arg0);
    java.lang.Class<?> resolveExtensionClass();
//    ExtensionRegistryLiteInterface newInstance();
//    void add(GeneratedExtensionInterface<?, ?> arg0);
    ExtensionInfoInterface findExtensionByNumber(DescriptorInterface arg0, int arg1);
    ExtensionInfoInterface findExtensionByName(java.lang.String arg0);
    void add(FieldDescriptorInterface arg0);
//    ExtensionRegistryLiteInterface getUnmodifiable();
    boolean isEagerlyParseMessageSets();
    ExtensionInfoInterface newExtensionInfo(ExtensionInterface<?, ?> arg0);
    java.util.Set<org.apache.hadoop.thirdparty.protobuf.ExtensionRegistry.ExtensionInfo> getAllMutableExtensionsByExtendedType(java.lang.String arg0);
}