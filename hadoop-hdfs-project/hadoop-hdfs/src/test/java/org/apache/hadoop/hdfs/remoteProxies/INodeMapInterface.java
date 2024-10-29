package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeWithAdditionalFields;

import java.util.*;
import java.io.*;

public interface INodeMapInterface {

    Iterator<INodeWithAdditionalFields> getMapIterator();

    void put(INode inode);

    void remove(INode inode);

    int size();

    INode get(long id);

    void clear();
}
