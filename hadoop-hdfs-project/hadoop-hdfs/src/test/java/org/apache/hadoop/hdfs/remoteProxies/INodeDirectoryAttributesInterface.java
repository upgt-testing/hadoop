package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.namenode.INodeDirectoryAttributes;

import java.util.*;
import java.io.*;

public interface INodeDirectoryAttributesInterface {

    QuotaCountsInterface getQuotaCounts();

    boolean metadataEquals(INodeDirectoryAttributes other);
}
