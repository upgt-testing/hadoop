package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;

import java.util.*;
import java.io.*;

public interface INodesInPathInterface {

    int getLatestSnapshotId();

    int getPathSnapshotId();

    INodeInterface getINode(int i);

    INodeInterface getLastINode();

    byte[][] getPathComponents();

    byte[] getPathComponent(int i);

    String getPath();

    String getParentPath();

    String getPath(int pos);

    int length();

    INodeInterface[] getINodesArray();

    INodesInPathInterface getParentINodesInPath();

    boolean isDescendant(INodeDirectory inodeDirectory);

    INodesInPathInterface getExistingINodes();

    boolean isRaw();

    String toString();
}
