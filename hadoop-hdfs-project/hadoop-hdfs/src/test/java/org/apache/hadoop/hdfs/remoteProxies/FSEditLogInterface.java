package org.apache.hadoop.hdfs.remoteProxies;

import java.util.*;
import java.io.*;

import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeFile;
import org.apache.hadoop.hdfs.server.protocol.RemoteEditLogManifest;
import org.apache.hadoop.hdfs.server.common.StorageInfo;

public interface FSEditLogInterface {

    void initJournalsForWrite();

    void initSharedJournalsForRead();

    boolean isOpenForRead();

    long getLastWrittenTxId();

    long getCurSegmentTxId();

    void logSync();

    void logAppendFile(String path, INodeFile file, boolean newBlock, boolean toLogRpcIds);

    void logOpenFile(String path, INodeFile newNode, boolean overwrite, boolean toLogRpcIds);

    void logCloseFile(String path, INodeFile newNode);

    void logAddBlock(String path, INodeFile file);

    void logUpdateBlocks(String path, INodeFile file, boolean toLogRpcIds);

    void logMkDir(String path, INode newNode);

    JournalSetInterface getJournalSet();

    RemoteEditLogManifest getEditLogManifest(long fromTxId);

    void startLogSegment(long txid, boolean abortCurrentLogSegment, int layoutVersion);

    void endCurrentLogSegment(boolean writeEndTxn);

    void purgeLogsOlderThan(long minTxIdToKeep);

    long getSyncTxId();

    void journal(long firstTxId, int numTxns, byte[] data);

    long getSharedLogCTime();

    void doPreUpgradeOfSharedLog();

    void doUpgradeOfSharedLog();

    void doFinalizeOfSharedLog();

    boolean canRollBackSharedLog(StorageInfo prevStorage, int targetLayoutVersion);

    void doRollback();

    void discardSegments(long markerTxid);

    //void selectInputStreams(Collection<EditLogInputStream> streams, long fromTxId, boolean inProgressOk, boolean onlyDurableTxns);

    //Collection<EditLogInputStream> selectInputStreams(long fromTxId, long toAtLeastTxId);

    //Collection<EditLogInputStream> selectInputStreams(long fromTxId, long toAtLeastTxId, MetaRecoveryContextInterface recovery, boolean inProgressOK);

    //Collection<EditLogInputStream> selectInputStreams(long fromTxId, long toAtLeastTxId, MetaRecoveryContext recovery, boolean inProgressOk, boolean onlyDurableTxns);

    // spy because a spy is a shallow copy
    void restart();

    long getTotalSyncCount();
}
