package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.AddBlockFlag;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo;
import org.apache.hadoop.net.Node;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;

public interface BlockPlacementPolicyInterface {

    DatanodeStorageInfoInterface[] chooseTarget(String srcPath, int numOfReplicas, Node writer, List<DatanodeStorageInfo> chosen, boolean returnChosenNodes, Set<Node> excludedNodes, long blocksize, BlockStoragePolicy storagePolicy, EnumSet<AddBlockFlag> flags);

    //DatanodeStorageInfoInterface[] chooseTarget(String srcPath, int numOfReplicas, Node writer, List<DatanodeStorageInfo> chosen, boolean returnChosenNodes, Set<Node> excludedNodes, long blocksize, BlockStoragePolicy storagePolicy, EnumSet<AddBlockFlag> flags, EnumMap<StorageType, Integer> storageTypes);

    BlockPlacementStatusInterface verifyBlockPlacement(DatanodeInfo[] locs, int numOfReplicas);

    //List<DatanodeStorageInfoInterface> chooseReplicasToDelete(Collection<DatanodeStorageInfo> availableReplicas, Collection<DatanodeStorageInfo> delCandidates, int expectedNumOfReplicas, List<StorageType> excessTypes, DatanodeDescriptor addedNode, DatanodeDescriptor delNodeHint);

    boolean isMovable(Collection<DatanodeInfo> candidates, DatanodeInfo source, DatanodeInfo target);

    void adjustSetsWithChosenReplica(Map<String, List<DatanodeStorageInfo>> rackMap, List<DatanodeStorageInfo> moreThanOne, List<DatanodeStorageInfo> exactlyOne, DatanodeStorageInfo cur);

    //void splitNodesWithRack(Iterable<T> availableSet, Collection<T> candidates, Map<String, List<T>> rackMap, List<T> moreThanOne, List<T> exactlyOne);

    void setExcludeSlowNodesEnabled(boolean enable);

    boolean getExcludeSlowNodesEnabled();
}
