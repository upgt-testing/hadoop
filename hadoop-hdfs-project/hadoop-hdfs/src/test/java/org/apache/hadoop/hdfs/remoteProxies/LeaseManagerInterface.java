package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.fs.BatchedRemoteIterator;
import org.apache.hadoop.hdfs.protocol.OpenFileEntry;
import org.apache.hadoop.hdfs.server.namenode.INodeDirectory;
import org.apache.hadoop.hdfs.server.namenode.INodesInPath;

import java.util.*;
import java.io.*;

public interface LeaseManagerInterface {

    Set<INodesInPath> getINodeWithLeases(INodeDirectory ancestorDir);

    BatchedRemoteIterator.BatchedListEntries<OpenFileEntry> getUnderConstructionFiles(long prevId);

    BatchedRemoteIterator.BatchedListEntries<OpenFileEntry> getUnderConstructionFiles(long prevId, String path);

    //Lease getLease(INodeFile src);

    int countLease();

    void setLeasePeriod(long softLimit, long hardLimit);

    String toString();

    void triggerMonitorCheckNow();

    void runLeaseChecks();
}
