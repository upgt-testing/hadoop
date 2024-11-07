package org.apache.hadoop.hdfs.remoteProxies;

public interface WriteFlusherInterface {
    boolean isTransitionAllowed(StateInterface arg0, StateInterface arg1);
    void completeWrite();
    org.eclipse.jetty.util.thread.Invocable.InvocationType getCallbackInvocationType();
//    boolean isState(org.eclipse.jetty.io.WriteFlusher.StateType arg0);
    boolean isFailed();
    boolean isIdle();
    void onClose();
    boolean updateState(StateInterface arg0, StateInterface arg1);
    void fail(org.eclipse.jetty.util.Callback arg0, java.lang.Throwable... arg1);
    boolean onFail(java.lang.Throwable arg0);
    boolean isPending();
    java.lang.String toStateString();
    void onIncompleteFlush();
    void write(org.eclipse.jetty.util.Callback arg0, java.nio.ByteBuffer... arg1) throws java.nio.channels.WritePendingException;
    java.nio.ByteBuffer[] flush(java.nio.ByteBuffer[] arg0) throws java.io.IOException;
    java.lang.String toString();
}