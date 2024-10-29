package org.apache.hadoop.hdfs.remoteProxies;

import java.net.URI;
import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.namenode.NNStorage;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;

public interface NNStorageInterface {

    boolean isPreUpgradableLayout(Storage.StorageDirectory sd);

    void close();

    Storage.StorageDirectory getStorageDirectory(URI uri);

    long getMostRecentCheckpointTxId();

    void writeTransactionIdFileToStorage(long txid);

    void writeTransactionIdFileToStorage(long txid, NNStorage.NameNodeDirType type);

    File[] getFsImageNameCheckpoint(long txid);

    File getFsImageName(long txid, NNStorage.NameNodeFile nnf);

    File getFsImage(long txid, EnumSet<NNStorage.NameNodeFile> nnfs);

    File getFsImageName(long txid);

    File getHighestFsImageName();

    void format(NamespaceInfo nsInfo);

    void format();

    void reportErrorOnFile(File f);

    String determineClusterId();

    void setBlockPoolID(String bpid);

    String getBlockPoolID();

    NamespaceInfo getNamespaceInfo();

    String getNNDirectorySize();

    void updateNameDirSize();

    void writeAll();
}
