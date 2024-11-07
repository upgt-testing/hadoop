package org.apache.hadoop.hdfs.remoteProxies;

public interface DelegationTokenSecretManagerMetricsInterface {
    void trackUpdateToken(org.apache.hadoop.util.functional.InvocationRaisingIOE arg0) throws java.io.IOException;
    MutableCounterLongInterface getTokenFailure();
    MutableRateInterface getRemoveToken();
    MutableRateInterface getUpdateToken();
    DelegationTokenSecretManagerMetricsInterface create();
    void trackStoreToken(org.apache.hadoop.util.functional.InvocationRaisingIOE arg0) throws java.io.IOException;
    org.apache.hadoop.fs.statistics.DurationTracker trackDuration(java.lang.String arg0, long arg1);
    MutableRateInterface getStoreToken();
    void trackRemoveToken(org.apache.hadoop.util.functional.InvocationRaisingIOE arg0) throws java.io.IOException;
    org.apache.hadoop.fs.statistics.DurationTracker trackDuration(java.lang.String arg0);
    org.apache.hadoop.fs.statistics.impl.IOStatisticsStore getIoStatistics();
    void trackInvocation(org.apache.hadoop.util.functional.InvocationRaisingIOE arg0, java.lang.String arg1, MutableRateInterface arg2) throws java.io.IOException;
}