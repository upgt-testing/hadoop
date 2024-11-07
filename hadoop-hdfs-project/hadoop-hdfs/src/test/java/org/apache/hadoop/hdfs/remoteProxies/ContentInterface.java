package org.apache.hadoop.hdfs.remoteProxies;

public interface ContentInterface {
    org.eclipse.jetty.util.Callback from(java.lang.Runnable arg0);
    org.eclipse.jetty.util.Callback from(org.eclipse.jetty.util.Callback arg0, java.lang.Runnable arg1);
    java.lang.String toString();
    void failed(java.lang.Throwable arg0);
    org.eclipse.jetty.util.thread.Invocable.InvocationType getInvocationType();
    int get(byte[] arg0, int arg1, int arg2);
    void succeeded();
    void invokeNonBlocking(java.lang.Runnable arg0);
    boolean isNonBlockingInvocation();
    java.nio.ByteBuffer getByteBuffer();
    org.eclipse.jetty.util.Callback from(org.eclipse.jetty.util.thread.Invocable.InvocationType arg0, java.lang.Runnable arg1, java.util.function.Consumer<java.lang.Throwable> arg2);
    org.eclipse.jetty.util.Callback from(java.util.concurrent.CompletableFuture<?> arg0, org.eclipse.jetty.util.thread.Invocable.InvocationType arg1);
    org.eclipse.jetty.util.Callback from(java.lang.Runnable arg0, org.eclipse.jetty.util.Callback arg1);
    org.eclipse.jetty.util.Callback from(java.util.concurrent.CompletableFuture<?> arg0);
    org.eclipse.jetty.util.thread.Invocable.InvocationType getInvocationType(java.lang.Object arg0);
    boolean hasContent();
    org.eclipse.jetty.util.thread.Invocable.InvocationType combine(org.eclipse.jetty.util.thread.Invocable.InvocationType arg0, org.eclipse.jetty.util.thread.Invocable.InvocationType arg1);
    org.eclipse.jetty.util.Callback from(java.lang.Runnable arg0, java.util.function.Consumer<java.lang.Throwable> arg1);
    org.eclipse.jetty.util.Callback combine(org.eclipse.jetty.util.Callback arg0, org.eclipse.jetty.util.Callback arg1);
    int skip(int arg0);
    int remaining();
    boolean isEmpty();
}