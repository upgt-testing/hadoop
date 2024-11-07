package org.apache.hadoop.hdfs.remoteProxies;

public interface InvalidProtocolBufferExceptionInterface {
    InvalidProtocolBufferExceptionInterface negativeSize();
    java.lang.Throwable fillInStackTrace();
    java.lang.Throwable fillInStackTrace(int arg0);
    InvalidProtocolBufferExceptionInterface recursionLimitExceeded();
    java.lang.String getLocalizedMessage();
//    void printStackTrace(java.io.PrintWriter arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    InvalidWireTypeExceptionInterface invalidWireType();
    java.lang.StackTraceElement[] getStackTrace();
    InvalidProtocolBufferExceptionInterface parseFailure();
    InvalidProtocolBufferExceptionInterface truncatedMessage();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.Throwable[] getSuppressed();
    java.lang.String getMessage();
    java.lang.Throwable getCause();
    InvalidProtocolBufferExceptionInterface invalidUtf8();
    void addSuppressed(java.lang.Throwable arg0);
    InvalidProtocolBufferExceptionInterface invalidEndTag();
    java.lang.StackTraceElement[] getOurStackTrace();
    void printStackTrace(java.io.PrintStream arg0);
    java.io.IOException unwrapIOException();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    InvalidProtocolBufferExceptionInterface invalidTag();
    InvalidProtocolBufferExceptionInterface malformedVarint();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    void printStackTrace();
    org.apache.hadoop.thirdparty.protobuf.MessageLite getUnfinishedMessage();
    java.lang.String toString();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    InvalidProtocolBufferExceptionInterface setUnfinishedMessage(org.apache.hadoop.thirdparty.protobuf.MessageLite arg0);
    InvalidProtocolBufferExceptionInterface sizeLimitExceeded();
}