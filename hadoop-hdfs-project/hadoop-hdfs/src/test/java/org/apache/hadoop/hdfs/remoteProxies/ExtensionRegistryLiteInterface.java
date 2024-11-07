package org.apache.hadoop.hdfs.remoteProxies;

public interface ExtensionRegistryLiteInterface {
    boolean isEagerlyParseMessageSets();
    void add(ExtensionLiteInterface<?, ?> arg0);
    void add(GeneratedExtensionInterface<?, ?> arg0);
    java.lang.Class<?> resolveExtensionClass();
    ExtensionRegistryLiteInterface getUnmodifiable();
    void setEagerlyParseMessageSets(boolean arg0);
    ExtensionRegistryLiteInterface getEmptyRegistry();
    ExtensionRegistryLiteInterface newInstance();
    <ContainingType> GeneratedExtensionInterface<ContainingType, ?> findLiteExtensionByNumber(ContainingType arg0, int arg1);
}