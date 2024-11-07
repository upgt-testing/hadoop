package org.apache.hadoop.hdfs.remoteProxies;

public interface InvalidWireTypeExceptionInterface {
    InvalidProtocolBufferExceptionInterface invalidTag();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    InvalidProtocolBufferExceptionInterface setUnfinishedMessage(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0);
    InvalidProtocolBufferExceptionInterface invalidUtf8();
    java.lang.Throwable fillInStackTrace();
    void printStackTrace(java.io.PrintStream arg0);
    java.lang.StackTraceElement[] getStackTrace();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    InvalidProtocolBufferExceptionInterface malformedVarint();
    InvalidProtocolBufferExceptionInterface truncatedMessage();
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    InvalidProtocolBufferExceptionInterface recursionLimitExceeded();
    java.io.IOException unwrapIOException();
    java.lang.Throwable[] getSuppressed();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    InvalidWireTypeExceptionInterface invalidWireType();
    InvalidProtocolBufferExceptionInterface parseFailure();
    void printStackTrace();
    java.lang.Throwable getCause();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    java.lang.String toString();
    org.apache.hadoop.thirdparty.protobuf.MessageLite getUnfinishedMessage();
    InvalidProtocolBufferExceptionInterface sizeLimitExceeded();
    java.lang.StackTraceElement[] getOurStackTrace();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    InvalidProtocolBufferExceptionInterface negativeSize();
    InvalidProtocolBufferExceptionInterface invalidEndTag();
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
//    void printStackTrace(java.io.PrintWriter arg0);
    java.lang.Throwable fillInStackTrace(int arg0);
    void addSuppressed(java.lang.Throwable arg0);
    java.lang.String getMessage();
    java.lang.String getLocalizedMessage();
}