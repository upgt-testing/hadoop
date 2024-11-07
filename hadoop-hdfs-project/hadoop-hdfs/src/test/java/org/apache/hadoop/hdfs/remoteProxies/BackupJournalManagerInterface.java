package org.apache.hadoop.hdfs.remoteProxies;

public interface BackupJournalManagerInterface {
    void format(NamespaceInfoInterface arg0, boolean arg1);
    void finalizeLogSegment(long arg0, long arg1) throws java.io.IOException;
    boolean hasSomeData();
    void doFinalize() throws java.io.IOException;
    EditLogOutputStreamInterface startLogSegment(long arg0, int arg1) throws java.io.IOException;
    void recoverUnfinalizedSegments() throws java.io.IOException;
    long getJournalCTime() throws java.io.IOException;
    void purgeLogsOlderThan(long arg0) throws java.io.IOException;
    void discardSegments(long arg0) throws java.io.IOException;
    boolean canRollBack(StorageInfoInterface arg0, StorageInfoInterface arg1, int arg2) throws java.io.IOException;
    void doRollback() throws java.io.IOException;
    void doUpgrade(StorageInterface arg0) throws java.io.IOException;
    void selectInputStreams(java.util.Collection<org.apache.hadoop.hdfs.server.namenode.EditLogInputStream> arg0, long arg1, boolean arg2, boolean arg3);
    java.lang.String toString();
    boolean matchesRegistration(NamenodeRegistrationInterface arg0);
    void setOutputBufferCapacity(int arg0);
    void doPreUpgrade() throws java.io.IOException;
    void close() throws java.io.IOException;
}