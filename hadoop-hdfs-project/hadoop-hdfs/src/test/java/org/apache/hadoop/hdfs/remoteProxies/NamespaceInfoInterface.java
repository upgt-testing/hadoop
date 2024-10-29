package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.ha.HAServiceProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;

import java.util.*;
import java.io.*;

public interface NamespaceInfoInterface {

    long getCapabilities();

    void setCapabilities(long capabilities);

    void setState(HAServiceProtocol.HAServiceState state);

    boolean isCapabilitySupported(NamespaceInfo.Capability capability);

    String getBuildVersion();

    String getBlockPoolID();

    String getSoftwareVersion();

    HAServiceProtocol.HAServiceState getState();

    void setClusterID(String clusterID);

    void setBlockPoolID(String blockPoolID);

    String toString();

    void validateStorage(NNStorageInterface storage);
}
