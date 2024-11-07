package org.apache.hadoop.hdfs.remoteProxies;

public interface StartupProgressViewInterface {
    java.lang.Iterable<org.apache.hadoop.hdfs.server.namenode.startupprogress.Step> getSteps(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    java.lang.Iterable<org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase> getPhases();
    long getCount(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    float getPercentComplete(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    long getElapsedTime(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    long getSize(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    java.lang.String getFile(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    StepTrackingInterface getStepTracking(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    long getElapsedTime(AbstractTrackingInterface arg0, AbstractTrackingInterface arg1);
    long getElapsedTime(AbstractTrackingInterface arg0);
    float getPercentComplete(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    long getTotal(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    org.apache.hadoop.hdfs.server.namenode.startupprogress.Status getStatus(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0);
    long getElapsedTime(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    float getBoundedPercent(float arg0);
    long getElapsedTime();
    long getCount(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
    float getPercentComplete();
    long getTotal(org.apache.hadoop.hdfs.server.namenode.startupprogress.Phase arg0, StepInterface arg1);
}