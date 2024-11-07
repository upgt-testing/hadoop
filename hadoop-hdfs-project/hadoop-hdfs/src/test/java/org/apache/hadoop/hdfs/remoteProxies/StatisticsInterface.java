package org.apache.hadoop.hdfs.remoteProxies;

public interface StatisticsInterface {
    void incrementBytesWritten(long arg0);
    int getReadOps();
    void incrementBytesRead(long arg0);
    int getLargeReadOps();
    void incrementBytesReadByDistance(int arg0, long arg1);
//    <T> T visitAll(org.apache.hadoop.fs.FileSystem.Statistics.StatisticsAggregator<T> arg0);
    int getAllThreadLocalDataSize();
    long getBytesRead();
    long getBytesWritten();
    void reset();
    long getBytesReadByDistance(int arg0);
    void incrementBytesReadErasureCoded(long arg0);
    long getBytesReadErasureCoded();
    java.lang.String toString();
    void incrementReadOps(int arg0);
    int getWriteOps();
    java.lang.String getScheme();
    StatisticsDataInterface getData();
    StatisticsDataInterface getThreadStatistics();
    void incrementWriteOps(int arg0);
    void incrementLargeReadOps(int arg0);
}