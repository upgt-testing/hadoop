package org.apache.hadoop.hdfs.remoteProxies;

public interface INodesInPathInterface {
    INodesInPathInterface append(INodesInPathInterface arg0, INodeInterface arg1, byte[] arg2);
    java.lang.String toString(INodeInterface arg0);
    INodesInPathInterface replace(INodesInPathInterface arg0, int arg1, INodeInterface arg2);
    byte[] getLastLocalName();
    int length();
    INodesInPathInterface getExistingINodes();
    INodesInPathInterface fromINode(INodeDirectoryInterface arg0, INodeInterface arg1);
    INodeInterface getLastINode();
    boolean isDotSnapshotDir(byte[] arg0);
    byte[] getPathComponent(int arg0);
    INodesInPathInterface getAncestorINodesInPath(int arg0);
    INodeInterface[] getINodes(INodeInterface arg0);
    byte[][] getPathComponents();
    INodeInterface[] getINodesArray();
    INodesInPathInterface fromINode(INodeInterface arg0);
    java.lang.String toString();
    INodeInterface getINode(int arg0);
    int getPathSnapshotId();
    void validate();
    INodesInPathInterface getParentINodesInPath();
    boolean isRaw();
    java.lang.String getPath(int arg0);
    java.lang.String toString(boolean arg0);
    INodesInPathInterface resolve(INodeDirectoryInterface arg0, byte[][] arg1, boolean arg2);
    boolean isDescendant(INodeDirectoryInterface arg0);
    java.lang.String getParentPath();
    boolean shouldUpdateLatestId(int arg0, int arg1);
    INodesInPathInterface resolve(INodeDirectoryInterface arg0, byte[][] arg1);
    INodesInPathInterface fromComponents(byte[][] arg0);
    java.lang.String getPath();
    boolean isDescendant(INodesInPathInterface arg0);
    byte[][] getPaths(INodeInterface[] arg0);
    int getLatestSnapshotId();
    boolean isSnapshot();
    boolean isDotSnapshotDir();
}