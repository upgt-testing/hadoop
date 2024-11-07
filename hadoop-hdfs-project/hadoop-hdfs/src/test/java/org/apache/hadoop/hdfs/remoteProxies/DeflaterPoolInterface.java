package org.apache.hadoop.hdfs.remoteProxies;

public interface DeflaterPoolInterface {
    DeflaterPoolInterface ensurePool(org.eclipse.jetty.util.component.Container arg0);
    boolean isRunning();
    java.util.zip.Deflater newObject();
    void end(java.util.zip.Deflater arg0);
    void setFailed(java.lang.Throwable arg0);
//    T acquire();
    void setStarting();
    void doStop();
    void setStopTimeout(long arg0);
    void setStopping();
    void setStarted();
    void stop(java.lang.Object arg0);
    void doStart() throws java.lang.Exception;
    java.lang.String toString();
    boolean isFailed();
//    void release(T arg0);
    java.lang.String getState();
    long getStopTimeout();
    boolean isStarting();
    boolean isStopping();
    void stop() throws java.lang.Exception;
//    T newObject();
    void addLifeCycleListener(org.eclipse.jetty.util.component.LifeCycle.Listener arg0);
    void setStopped();
    java.lang.String getState(org.eclipse.jetty.util.component.LifeCycle arg0);
    void start() throws java.lang.Exception;
    void start(java.lang.Object arg0);
    boolean isStopped();
    boolean isStarted();
    void removeLifeCycleListener(org.eclipse.jetty.util.component.LifeCycle.Listener arg0);
    void reset(java.util.zip.Deflater arg0);
}