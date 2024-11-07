package org.apache.hadoop.hdfs.remoteProxies;

public interface BlockerInterface {
    org.eclipse.jetty.util.Callback from(java.lang.Runnable arg0, org.eclipse.jetty.util.Callback arg1);
    org.eclipse.jetty.util.thread.Invocable.InvocationType getInvocationType(java.lang.Object arg0);
    org.eclipse.jetty.util.Callback from(java.lang.Runnable arg0);
    void invokeNonBlocking(java.lang.Runnable arg0);
    void block() throws java.io.IOException;
    org.eclipse.jetty.util.Callback combine(org.eclipse.jetty.util.Callback arg0, org.eclipse.jetty.util.Callback arg1);
    void close();
    org.eclipse.jetty.util.thread.Invocable.InvocationType getInvocationType();
    org.eclipse.jetty.util.Callback from(java.util.concurrent.CompletableFuture<?> arg0, org.eclipse.jetty.util.thread.Invocable.InvocationType arg1);
    void failed(java.lang.Throwable arg0);
    void succeeded();
    org.eclipse.jetty.util.Callback from(java.lang.Runnable arg0, java.util.function.Consumer<java.lang.Throwable> arg1);
    org.eclipse.jetty.util.Callback from(org.eclipse.jetty.util.Callback arg0, java.lang.Runnable arg1);
    org.eclipse.jetty.util.Callback from(org.eclipse.jetty.util.thread.Invocable.InvocationType arg0, java.lang.Runnable arg1, java.util.function.Consumer<java.lang.Throwable> arg2);
    boolean isNonBlockingInvocation();
    org.eclipse.jetty.util.Callback from(java.util.concurrent.CompletableFuture<?> arg0);
    org.eclipse.jetty.util.thread.Invocable.InvocationType combine(org.eclipse.jetty.util.thread.Invocable.InvocationType arg0, org.eclipse.jetty.util.thread.Invocable.InvocationType arg1);
    java.lang.String toString();
}