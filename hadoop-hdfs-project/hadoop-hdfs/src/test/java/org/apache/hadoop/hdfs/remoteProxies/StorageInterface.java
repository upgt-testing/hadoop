package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;

public interface StorageInterface {

    List<File> getFiles(Storage.StorageDirType dirType, String fileName);

    Iterator<Storage.StorageDirectory> dirIterator();

    Iterator<Storage.StorageDirectory> dirIterator(Storage.StorageDirType dirType);

    Iterator<Storage.StorageDirectory> dirIterator(boolean includeShared);

    Iterator<Storage.StorageDirectory> dirIterator(Storage.StorageDirType dirType, boolean includeShared);

    Iterable<Storage.StorageDirectory> dirIterable(Storage.StorageDirType dirType);

    String listStorageDirectories();

    int getNumStorageDirs();

    List<Storage.StorageDirectory> getStorageDirs();

    Storage.StorageDirectory getStorageDir(int idx);

    Storage.StorageDirectory getSingularStorageDir();

    NamespaceInfo getNamespaceInfo();

    boolean isPreUpgradableLayout(Storage.StorageDirectory sd);

    void writeProperties(Storage.StorageDirectory sd);

    void writeProperties(File to, Storage.StorageDirectory sd);

    void writeAll();

    void unlockAll();
}
