package org.apache.hadoop.hdfs.remoteProxies;

public interface RemoteExceptionInterface {
    java.io.IOException unwrapRemoteException(java.lang.Class<?>... arg0);
    java.lang.String getClassName();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    java.lang.Throwable getCause();
    java.lang.Throwable fillInStackTrace();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.Throwable[] getSuppressed();
    java.lang.Throwable fillInStackTrace(int arg0);
    void printStackTrace();
    void addSuppressed(java.lang.Throwable arg0);
    java.lang.String toString();
    java.lang.String getLocalizedMessage();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    RemoteExceptionInterface valueOf(org.xml.sax.Attributes arg0);
    void printStackTrace(java.io.PrintStream arg0);
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    java.lang.StackTraceElement[] getOurStackTrace();
//    void printStackTrace(java.io.PrintWriter arg0);
    java.io.IOException unwrapRemoteException();
    java.lang.String getMessage();
    java.io.IOException instantiateException(java.lang.Class<? extends java.io.IOException> arg0) throws java.lang.Exception;
    java.lang.StackTraceElement[] getStackTrace();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    org.apache.hadoop.ipc.protobuf.RpcHeaderProtos.RpcResponseHeaderProto.RpcErrorCodeProto getErrorCode();
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
}