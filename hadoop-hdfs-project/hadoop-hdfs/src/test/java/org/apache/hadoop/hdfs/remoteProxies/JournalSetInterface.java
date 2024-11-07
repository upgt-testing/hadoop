package org.apache.hadoop.hdfs.remoteProxies;

public interface JournalSetInterface {
    void doPreUpgrade() throws java.io.IOException;
    void chainAndMakeRedundantStreams(java.util.Collection<org.apache.hadoop.hdfs.server.namenode.EditLogInputStream> arg0, java.util.PriorityQueue<org.apache.hadoop.hdfs.server.namenode.EditLogInputStream> arg1, long arg2);
    java.util.List<org.apache.hadoop.hdfs.server.namenode.JournalManager> getJournalManagers();
    void format(NamespaceInfoInterface arg0, boolean arg1) throws java.io.IOException;
    boolean hasSomeData() throws java.io.IOException;
    void remove(org.apache.hadoop.hdfs.server.namenode.JournalManager arg0);
    void add(org.apache.hadoop.hdfs.server.namenode.JournalManager arg0, boolean arg1, boolean arg2);
    boolean canRollBack(StorageInfoInterface arg0, StorageInfoInterface arg1, int arg2) throws java.io.IOException;
    boolean isEmpty();
    void close() throws java.io.IOException;
    void discardSegments(long arg0) throws java.io.IOException;
    void recoverUnfinalizedSegments() throws java.io.IOException;
    void doRollback() throws java.io.IOException;
    void purgeLogsOlderThan(long arg0) throws java.io.IOException;
    void abortAllJournals();
//    java.util.List<org.apache.hadoop.hdfs.server.namenode.JournalSet.JournalAndStream> getAllJournalStreams();
    void doFinalize() throws java.io.IOException;
    EditLogOutputStreamInterface startLogSegment(long arg0, int arg1) throws java.io.IOException;
    void finalizeLogSegment(long arg0, long arg1) throws java.io.IOException;
    void selectInputStreams(java.util.Collection<org.apache.hadoop.hdfs.server.namenode.EditLogInputStream> arg0, long arg1, boolean arg2, boolean arg3);
//    void mapJournalsAndReportErrors(org.apache.hadoop.hdfs.server.namenode.JournalSet.JournalClosure arg0, java.lang.String arg1) throws java.io.IOException;
    RemoteEditLogManifestInterface getEditLogManifest(long arg0);
    java.lang.String getSyncTimes();
    long getJournalCTime() throws java.io.IOException;
    boolean isOpen();
    void add(org.apache.hadoop.hdfs.server.namenode.JournalManager arg0, boolean arg1);
    void setOutputBufferCapacity(int arg0);
//    void disableAndReportErrorOnJournals(java.util.List<org.apache.hadoop.hdfs.server.namenode.JournalSet.JournalAndStream> arg0);
    void doUpgrade(StorageInterface arg0) throws java.io.IOException;
}