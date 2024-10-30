package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;

import java.util.*;
import java.io.*;

public interface ReplicaRecoveryInfoInterface {

    HdfsServerConstants.ReplicaState getOriginalReplicaState();

    boolean equals(Object o);

    int hashCode();

    String toString();
}
