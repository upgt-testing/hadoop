package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;
import org.apache.hadoop.hdfs.server.common.StorageInfo;
import org.apache.hadoop.hdfs.server.namenode.EditLogInputStream;

public interface JournalSetInterface {

    void format(NamespaceInfoInterface nsInfo, boolean force);

    boolean hasSomeData();

    EditLogOutputStreamInterface startLogSegment(long txId, int layoutVersion);

    void finalizeLogSegment(long firstTxId, long lastTxId);

    void close();

    boolean isOpen();

    void selectInputStreams(Collection<EditLogInputStream> streams, long fromTxId, boolean inProgressOk, boolean onlyDurableTxns);

    boolean isEmpty();

    void setOutputBufferCapacity(int size);

    void purgeLogsOlderThan(long minTxIdToKeep);

    void recoverUnfinalizedSegments();

    RemoteEditLogManifestInterface getEditLogManifest(long fromTxId);

    void doPreUpgrade();

    void doUpgrade(StorageInterface storage);

    void doFinalize();

    boolean canRollBack(StorageInfoInterface storage, StorageInfo prevStorage, int targetLayoutVersion);

    void doRollback();

    void discardSegments(long startTxId);

    long getJournalCTime();
}
