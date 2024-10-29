package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.net.Node;

import java.util.*;
import java.io.*;

public interface NodeInterface {

    String getNetworkLocation();

    void setNetworkLocation(String location);

    String getName();

    NodeInterface getParent();

    void setParent(Node parent);

    int getLevel();

    void setLevel(int i);
}
