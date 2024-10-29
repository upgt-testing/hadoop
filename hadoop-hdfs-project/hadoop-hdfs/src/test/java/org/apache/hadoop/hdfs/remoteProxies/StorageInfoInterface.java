package org.apache.hadoop.hdfs.remoteProxies;

import org.apache.hadoop.hdfs.protocol.LayoutVersion;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.common.StorageInfo;

import java.util.*;
import java.io.*;

public interface StorageInfoInterface {

    int getLayoutVersion();

    int getNamespaceID();

    String getClusterID();

    long getCTime();

    void setStorageInfo(StorageInfo from);

    boolean versionSupportsFederation(Map<Integer, SortedSet<LayoutVersion.LayoutFeature>> map);

    String toString();

    String toColonSeparatedString();

    void readProperties(Storage.StorageDirectory sd);

    void readPreviousVersionProperties(Storage.StorageDirectory sd);

    void setServiceLayoutVersion(int lv);

    int getServiceLayoutVersion();

    Map<Integer, SortedSet<LayoutVersion.LayoutFeature>> getServiceLayoutFeatureMap();
}
