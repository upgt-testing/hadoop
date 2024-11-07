package org.apache.hadoop.hdfs.remoteProxies;

public interface UninitializedMessageExceptionInterface {
    void addSuppressed(java.lang.Throwable arg0);
    java.lang.Throwable fillInStackTrace(int arg0);
    java.lang.String getLocalizedMessage();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    InvalidProtocolBufferExceptionInterface asInvalidProtocolBufferException();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    void printStackTrace(java.io.PrintStream arg0);
    java.lang.StackTraceElement[] getOurStackTrace();
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    void printStackTrace(java.io.PrintWriter arg0);
    java.lang.StackTraceElement[] getStackTrace();
    java.lang.Throwable fillInStackTrace();
    java.lang.String buildDescription(java.util.List<java.lang.String> arg0);
    java.lang.String toString();
    java.lang.Throwable getCause();
    void printStackTrace();
    java.lang.String getMessage();
    java.util.List<java.lang.String> getMissingFields();
    java.lang.Throwable[] getSuppressed();
}