package org.apache.hadoop.hdfs.remoteProxies;

public interface ProvidedStorageMapInterface {
    void removeDatanode(DatanodeDescriptorInterface arg0);
    boolean isProvidedStorage(java.lang.String arg0);
    void updateStorage(DatanodeDescriptorInterface arg0, DatanodeStorageInterface arg1);
    void processProvidedStorageReport() throws java.io.IOException;
    long getCapacity();
    BlockAliasMapInterface getAliasMap();
    DatanodeStorageInfoInterface getStorage(DatanodeDescriptorInterface arg0, DatanodeStorageInterface arg1) throws java.io.IOException;
    DatanodeStorageInfoInterface getProvidedStorageInfo();
    DatanodeDescriptorInterface chooseProvidedDatanode();
    LocatedBlockBuilderInterface newLocatedBlocks(int arg0);
}