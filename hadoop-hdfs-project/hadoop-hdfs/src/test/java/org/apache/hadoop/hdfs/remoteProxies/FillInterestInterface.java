package org.apache.hadoop.hdfs.remoteProxies;

public interface FillInterestInterface {
    void needsFillInterest() throws java.io.IOException;
    boolean tryRegister(org.eclipse.jetty.util.Callback arg0);
    boolean onFail(java.lang.Throwable arg0);
    void register(org.eclipse.jetty.util.Callback arg0) throws java.nio.channels.ReadPendingException;
    void onClose();
    java.lang.String toStateString();
    boolean isInterested();
    boolean fillable();
    org.eclipse.jetty.util.thread.Invocable.InvocationType getCallbackInvocationType();
    java.lang.String toString();
}