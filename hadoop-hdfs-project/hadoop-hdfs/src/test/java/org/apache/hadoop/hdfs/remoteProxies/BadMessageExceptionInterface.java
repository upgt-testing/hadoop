package org.apache.hadoop.hdfs.remoteProxies;

public interface BadMessageExceptionInterface {
    java.lang.Throwable fillInStackTrace();
//    void printStackTrace(java.io.PrintStream arg0);
    java.lang.StackTraceElement[] getStackTrace();
    java.lang.StackTraceElement[] getOurStackTrace();
    java.lang.String getMessage();
    void addSuppressed(java.lang.Throwable arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    void printStackTrace(java.io.PrintWriter arg0);
    java.lang.String getReason();
    int getCode();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
    java.lang.String getLocalizedMessage();
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
    java.lang.String toString();
    java.lang.Throwable fillInStackTrace(int arg0);
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    void printStackTrace();
    java.lang.Throwable[] getSuppressed();
    java.lang.Throwable getCause();
    java.lang.Throwable initCause(java.lang.Throwable arg0);
}