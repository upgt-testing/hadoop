package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.net.Node;

import java.util.*;
import java.io.*;

public interface NetworkTopologyInterface {

    void add(NodeInterface node);

    List<NodeInterface> getDatanodesInRack(String loc);

    void remove(Node node);

    boolean contains(Node node);

    NodeInterface getNode(String loc);

    boolean hasClusterEverBeenMultiRack();

    String getRack(String loc);

    int getNumOfRacks();

    int getNumOfLeaves();

    int getDistance(Node node1, Node node2);

    boolean isOnSameRack(Node node1, Node node2);

    boolean isNodeGroupAware();

    boolean isOnSameNodeGroup(Node node1, Node node2);

    NodeInterface chooseRandom(String scope);

    NodeInterface chooseRandom(String scope, Collection<Node> excludedNodes);

    List<NodeInterface> getLeaves(String scope);

    int countNumOfAvailableNodes(String scope, Collection<Node> excludedNodes);

    String toString();

    void sortByDistance(Node reader, Node[] nodes, int activeLen);

    //void sortByDistance(Node reader, T[] nodes, int activeLen, Consumer<List<T>> secondarySort);

    void sortByDistanceUsingNetworkLocation(Node reader, Node[] nodes, int activeLen);

    //void sortByDistanceUsingNetworkLocation(Node reader, T[] nodes, int activeLen, Consumer<List<T>> secondarySort);

    int getNumOfNonEmptyRacks();

    void recommissionNode(Node node);

    void decommissionNode(Node node);
}
