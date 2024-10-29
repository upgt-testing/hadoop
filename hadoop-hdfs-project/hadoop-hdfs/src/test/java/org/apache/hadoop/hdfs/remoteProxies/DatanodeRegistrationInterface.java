package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.common.StorageInfo;
import org.apache.hadoop.hdfs.security.token.block.ExportedBlockKeys;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;

public interface DatanodeRegistrationInterface {

    StorageInfoInterface getStorageInfo();

    void setExportedKeys(ExportedBlockKeys keys);

    ExportedBlockKeysInterface getExportedKeys();

    String getSoftwareVersion();

    int getVersion();

    void setNamespaceInfo(NamespaceInfo nsInfo);

    NamespaceInfoInterface getNamespaceInfo();

    String getRegistrationID();

    String getAddress();

    String toString();

    boolean equals(Object to);

    int hashCode();
}
