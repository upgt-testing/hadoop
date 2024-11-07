package org.apache.hadoop.hdfs.remoteProxies;

public interface StartupProgressInterface {
    StartupProgressViewInterface createView();
    void endStep(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    StepTrackingInterface lazyInitStep(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    boolean isComplete();
    void setCount(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1, long arg2);
    org.apache.hadoop.hdfs.server.namenode.startupprogress.Status getStatus(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    void beginStep(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    void beginPhase(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    void endPhase(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    org.apache.hadoop.hdfs.server.namenode.startupprogress.StartupProgress.Counter getCounter(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    void setSize(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, long arg1);
    void setFile(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, java.lang.String arg1);
    boolean isComplete(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    void setTotal(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1, long arg2);
}