package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.hdfs.server.namenode.EditLogInputStream;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.hdfs.server.namenode.MetaRecoveryContext;
import org.apache.hadoop.hdfs.server.namenode.NNStorage;
import org.apache.hadoop.hdfs.util.Canceler;

public interface FSImageInterface {

    boolean hasRollbackFSImage();

    FSEditLogInterface getEditLog();

    void initEditLog(HdfsServerConstants.StartupOption startOpt);

    long loadEdits(Iterable<EditLogInputStream> editStreams, FSNamesystem target);

    long loadEdits(Iterable<EditLogInputStream> editStreams, FSNamesystem target, long maxTxnsToRead, HdfsServerConstants.StartupOption startOpt, MetaRecoveryContext recovery);

    void saveLegacyOIVImage(FSNamesystem source, String targetDir, CancelerInterface canceler);

    void updateStorageVersion();

    boolean saveNamespace(long timeWindow, long txGap, FSNamesystem source);

    void saveNamespace(FSNamesystem source);

    void saveNamespace(FSNamesystem source, NNStorage.NameNodeFile nnf, Canceler canceler);

    boolean addToCheckpointing(long txid);

    void removeFromCheckpointing(long txid);

    void saveDigestAndRenameCheckpointImage(NNStorage.NameNodeFile nnf, long txid, MD5HashInterface digest);

    void close();

    NNStorageInterface getStorage();

    int getLayoutVersion();

    int getNamespaceID();

    String getClusterID();

    String getBlockPoolID();

    long getLastAppliedTxId();

    long getLastAppliedOrWrittenTxId();

    long getCorrectLastAppliedOrWrittenTxId();

    void updateLastAppliedTxIdFromWritten();

    long getMostRecentCheckpointTxId();
}
