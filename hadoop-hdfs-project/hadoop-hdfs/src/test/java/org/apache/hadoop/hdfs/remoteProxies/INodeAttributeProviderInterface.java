package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributes;

import java.util.*;
import java.io.*;

public interface INodeAttributeProviderInterface {

    void start();

    void stop();

    INodeAttributesInterface getAttributes(String fullPath, INodeAttributes inode);

    INodeAttributesInterface getAttributes(String[] pathElements, INodeAttributes inode);

    INodeAttributesInterface getAttributes(byte[][] components, INodeAttributes inode);

    //AccessControlEnforcer getExternalAccessControlEnforcer(INodeAttributeProvider.AccessControlEnforcer defaultEnforcer);
}
