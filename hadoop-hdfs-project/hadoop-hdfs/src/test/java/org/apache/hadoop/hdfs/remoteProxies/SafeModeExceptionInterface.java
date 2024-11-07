package org.apache.hadoop.hdfs.remoteProxies;

public interface SafeModeExceptionInterface {
    java.lang.Throwable fillInStackTrace();
//    void printStackTrace(java.io.PrintWriter arg0);
    java.lang.StackTraceElement[] getOurStackTrace();
    java.lang.StackTraceElement[] getStackTrace();
    java.lang.Throwable fillInStackTrace(int arg0);
    java.lang.Throwable[] getSuppressed();
    java.lang.String getMessage();
    java.lang.String toString();
    void printStackTrace(java.io.PrintStream arg0);
    java.lang.String getLocalizedMessage();
//    void printStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0);
    void readObject(java.io.ObjectInputStream arg0) throws java.io.IOException, java.lang.ClassNotFoundException;
    void printStackTrace();
    java.lang.Throwable getCause();
    void setStackTrace(java.lang.StackTraceElement[] arg0);
    java.lang.Throwable initCause(java.lang.Throwable arg0);
    void addSuppressed(java.lang.Throwable arg0);
    void writeObject(java.io.ObjectOutputStream arg0) throws java.io.IOException;
//    void printEnclosedStackTrace(java.lang.Throwable.PrintStreamOrWriter arg0, java.lang.StackTraceElement[] arg1, java.lang.String arg2, java.lang.String arg3, java.util.Set<java.lang.Throwable> arg4);
    int validateSuppressedExceptionsList(java.util.List<java.lang.Throwable> arg0) throws java.io.IOException;
}