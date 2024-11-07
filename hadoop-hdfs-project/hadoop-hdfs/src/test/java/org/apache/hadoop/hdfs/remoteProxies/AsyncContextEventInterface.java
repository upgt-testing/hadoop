package org.apache.hadoop.hdfs.remoteProxies;

public interface AsyncContextEventInterface {
    void completed();
    void setTimeoutTask(org.eclipse.jetty.util.thread.Scheduler.Task arg0);
    boolean hasTimeoutTask();
    ContextInterface getContext();
    javax.servlet.ServletContext getSuspendedContext();
    java.lang.Throwable getThrowable();
    java.lang.String getPath();
    javax.servlet.ServletResponse getSuppliedResponse();
    javax.servlet.ServletRequest getSuppliedRequest();
    javax.servlet.ServletContext getDispatchContext();
    void setDispatchContext(javax.servlet.ServletContext arg0);
    javax.servlet.ServletContext getServletContext();
    void addThrowable(java.lang.Throwable arg0);
    javax.servlet.AsyncContext getAsyncContext();
    void run();
    HttpChannelStateInterface getHttpChannelState();
    void setDispatchPath(java.lang.String arg0);
    void cancelTimeoutTask();
}