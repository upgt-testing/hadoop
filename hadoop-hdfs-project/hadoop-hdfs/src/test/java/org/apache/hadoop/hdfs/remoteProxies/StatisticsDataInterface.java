package org.apache.hadoop.hdfs.remoteProxies;

public interface StatisticsDataInterface {
    long getBytesReadErasureCoded();
    java.lang.String toString();
    void add(StatisticsDataInterface arg0);
    int getReadOps();
    int getLargeReadOps();
    long getBytesReadLocalHost();
    int getWriteOps();
    long getBytesReadDistanceOfOneOrTwo();
    void negate();
    long getBytesReadDistanceOfFiveOrLarger();
    long getBytesRead();
    long getBytesReadDistanceOfThreeOrFour();
    long getBytesWritten();
}