package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.protocol.DatanodeStorage;

public interface ProvidedStorageMapInterface {

    DatanodeStorageInfoInterface getProvidedStorageInfo();

    LocatedBlockBuilderInterface newLocatedBlocks(int maxValue);

    void removeDatanode(DatanodeDescriptor dnToRemove);

    long getCapacity();

    void updateStorage(DatanodeDescriptor node, DatanodeStorage storage);

    DatanodeDescriptorInterface chooseProvidedDatanode();

    BlockAliasMapInterface getAliasMap();
}
